// Compatibility shim for DMSLib's single HashCode.Combine call.  System.HashCode
// was added after .NET Framework 4.0; keeping it here avoids modifying upstream.
namespace System
{
    public static class HashCode
    {
        public static int Combine<T1, T2, T3, T4>(T1 value1, T2 value2, T3 value3, T4 value4)
        {
            unchecked
            {
                int hash = 17;
                hash = hash * 31 + (value1 == null ? 0 : value1.GetHashCode());
                hash = hash * 31 + (value2 == null ? 0 : value2.GetHashCode());
                hash = hash * 31 + (value3 == null ? 0 : value3.GetHashCode());
                hash = hash * 31 + (value4 == null ? 0 : value4.GetHashCode());
                return hash;
            }
        }
    }
}
