package Proxy.main;

public class Debug {

    //Print DEBUG info
    public static final boolean DEBUG = true;
    //Print DEBUG read or write bytes info
    public static final boolean BYTES = false;
    //Print DEBUG open sockets number
    public static final boolean OPEN = false;
    //Print DEBUG all open sockets info
    public static final boolean OPEN_INFO = false;

    public static void println(String str) {
        if (DEBUG) {
            System.out.println(str);
        }
    }

    public static void print(String str) {
        if (DEBUG) {
            System.out.print(str);
        }
    }

    public static void bytesPrintln(String str) {
        if (BYTES) {
            System.out.println(str);
        }
    }

    public static void bytesPrint(String str) {
        if (BYTES) {
            System.out.print(str);
        }
    }

    public static void openPrintln(String str) {
        if (OPEN) {
            System.out.println(str);
        }
    }

    public static void openInfoPrintln(String str) {
        if (OPEN_INFO) {
            System.out.println(str);
        }
    }

    public static void openInfoPrint(String str) {
        if (OPEN_INFO) {
            System.out.print(str);
        }
    }
}
