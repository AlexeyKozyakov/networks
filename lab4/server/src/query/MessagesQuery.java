package query;

public class MessagesQuery {
    private int offset;
    private int count;

    public static MessagesQuery fromQueryStr(String queryStr) {
        int offset = 0;
        int count = 10;
        String [] args = queryStr.split("&");
        for (String arg : args) {
            if (arg.startsWith("offset")) {
                String [] offsetArg = arg.split("=");
                if (offsetArg.length != 2) {
                    return null;
                }
                try {
                    offset = Integer.valueOf(offsetArg[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            if (arg.startsWith("count")) {
                String [] countArg = arg.split("=");
                if (countArg.length != 2) {
                    return null;
                }
                try {
                    count = Integer.valueOf(countArg[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return new MessagesQuery(offset, count);
    }

    public MessagesQuery(int offset, int count) {
        this.offset = offset;
        this.count = count;
    }

    public int getOffset() {
        return offset;
    }

    public int getCount() {
        return count;
    }
}
