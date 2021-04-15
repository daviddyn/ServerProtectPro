import com.davidsoft.net.RegexIP;

import java.text.ParseException;

public final class Debug {

    public static void main(String[] args) throws ParseException {
        System.out.println(RegexIP.toString(RegexIP.parse("*.*.*.*")));
    }
}
