package com.gideontaylor.peoplesoft.extractor.gui;

import java.awt.Window;
import java.io.File;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.COM.COMUtils;
import com.sun.jna.platform.win32.COM.Unknown;
import com.sun.jna.platform.win32.Guid.CLSID;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Guid.IID;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

final class WindowsNativeDialogs {
    private static final CLSID CLSID_FILE_OPEN_DIALOG = new CLSID("{DC1C5A9C-E88A-4DDE-A5A1-60F82A20AEF7}");
    private static final IID IID_FILE_OPEN_DIALOG = new IID("{D57C7288-D4AD-4768-BE02-9D969532D960}");
    private static final IID IID_SHELL_ITEM = new IID("{43826D1E-E718-42EE-BC55-A1E261C37BFE}");
    private static final int FOS_PICKFOLDERS = 0x00000020;
    private static final int FOS_FORCEFILESYSTEM = 0x00000040;
    private static final int FOS_PATHMUSTEXIST = 0x00000800;
    private static final int FOS_FILEMUSTEXIST = 0x00001000;
    private static final int SIGDN_FILESYSPATH = 0x80058000;
    private static final int ERROR_CANCELLED_HRESULT = 0x800704C7;

    private WindowsNativeDialogs() {}

    static File chooseXmlFile(Window owner, String title, File initialLocation) {
        return choose(owner, title, initialLocation, false);
    }

    static File chooseFolder(Window owner, String title, File initialLocation) {
        return choose(owner, title, initialLocation, true);
    }

    private static File choose(Window owner, String title, File initialLocation, boolean folder) {
        HRESULT initialized = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_APARTMENTTHREADED);
        boolean uninitialize = COMUtils.SUCCEEDED(initialized);
        FileDialogCom dialog = null;
        ShellItemCom result = null;
        Pointer allocatedPath = null;
        try {
            PointerByReference created = new PointerByReference();
            COMUtils.checkRC(Ole32.INSTANCE.CoCreateInstance(CLSID_FILE_OPEN_DIALOG, null,
                    WTypes.CLSCTX_INPROC_SERVER, IID_FILE_OPEN_DIALOG, created));
            dialog = new FileDialogCom(created.getValue());

            IntByReference options = new IntByReference();
            COMUtils.checkRC(dialog.getOptions(options));
            int requested = options.getValue() | FOS_FORCEFILESYSTEM | FOS_PATHMUSTEXIST;
            requested |= folder ? FOS_PICKFOLDERS : FOS_FILEMUSTEXIST;
            COMUtils.checkRC(dialog.setOptions(requested));
            if (!folder) {
                FileTypeFilter xmlFilter = new FileTypeFilter(new WString("XML files (*.xml)"), new WString("*.xml"));
                xmlFilter.write();
                COMUtils.checkRC(dialog.setFileTypes(1, xmlFilter.getPointer()));
                COMUtils.checkRC(dialog.setFileTypeIndex(1));
            }
            if (title != null && !title.isBlank()) COMUtils.checkRC(dialog.setTitle(new WString(title)));
            setInitialFolder(dialog, initialLocation);

            Pointer ownerPointer = owner == null || !owner.isDisplayable() ? null : Native.getComponentPointer(owner);
            HRESULT shown = dialog.show(ownerPointer);
            if (shown.intValue() == ERROR_CANCELLED_HRESULT) return null;
            COMUtils.checkRC(shown);

            PointerByReference selected = new PointerByReference();
            COMUtils.checkRC(dialog.getResult(selected));
            result = new ShellItemCom(selected.getValue());
            PointerByReference displayName = new PointerByReference();
            COMUtils.checkRC(result.getDisplayName(SIGDN_FILESYSPATH, displayName));
            allocatedPath = displayName.getValue();
            return new File(allocatedPath.getWideString(0));
        } finally {
            if (allocatedPath != null) Ole32.INSTANCE.CoTaskMemFree(allocatedPath);
            if (result != null) result.Release();
            if (dialog != null) dialog.Release();
            if (uninitialize) Ole32.INSTANCE.CoUninitialize();
        }
    }

    private static void setInitialFolder(FileDialogCom dialog, File location) {
        if (location == null) return;
        File folder = location.isDirectory() ? location : location.getParentFile();
        if (folder == null || !folder.isDirectory()) return;
        PointerByReference shellItemPointer = new PointerByReference();
        HRESULT created = Shell32Ex.INSTANCE.SHCreateItemFromParsingName(new WString(folder.getAbsolutePath()),
                null, IID_SHELL_ITEM, shellItemPointer);
        if (COMUtils.FAILED(created)) return;
        Unknown shellItem = new Unknown(shellItemPointer.getValue());
        try {
            dialog.setFolder(shellItem.getPointer());
        } finally {
            shellItem.Release();
        }
    }

    private interface Shell32Ex extends StdCallLibrary {
        Shell32Ex INSTANCE = Native.load("shell32", Shell32Ex.class, W32APIOptions.UNICODE_OPTIONS);
        HRESULT SHCreateItemFromParsingName(WString path, Pointer bindingContext, GUID interfaceId,
                PointerByReference shellItem);
    }

    private static final class FileDialogCom extends Unknown {
        FileDialogCom(Pointer pointer) { super(pointer); }
        HRESULT show(Pointer owner) { return invoke(3, owner); }
        HRESULT setFileTypes(int count, Pointer filters) { return invoke(4, count, filters); }
        HRESULT setFileTypeIndex(int index) { return invoke(5, index); }
        HRESULT setOptions(int options) { return invoke(9, options); }
        HRESULT getOptions(IntByReference options) { return invoke(10, options); }
        HRESULT setFolder(Pointer shellItem) { return invoke(12, shellItem); }
        HRESULT setTitle(WString title) { return invoke(17, title); }
        HRESULT getResult(PointerByReference result) { return invoke(20, result); }
        private HRESULT invoke(int index, Object... values) {
            Object[] args = new Object[values.length + 1];
            args[0] = getPointer();
            System.arraycopy(values, 0, args, 1, values.length);
            return (HRESULT) _invokeNativeObject(index, args, HRESULT.class);
        }
    }

    @Structure.FieldOrder({ "displayName", "pattern" })
    public static final class FileTypeFilter extends Structure {
        public WString displayName;
        public WString pattern;
        public FileTypeFilter() {}
        FileTypeFilter(WString displayName, WString pattern) {
            this.displayName = displayName;
            this.pattern = pattern;
        }
    }

    private static final class ShellItemCom extends Unknown {
        ShellItemCom(Pointer pointer) { super(pointer); }
        HRESULT getDisplayName(int nameType, PointerByReference value) {
            return (HRESULT) _invokeNativeObject(5, new Object[] { getPointer(), nameType, value }, HRESULT.class);
        }
    }
}
