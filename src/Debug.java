import com.davidsoft.net.NetURI;
import com.davidsoft.net.RegexIP;
import com.davidsoft.url.URI;
import com.davidsoft.url.URIIndex;

import java.text.ParseException;

public final class Debug {

    public static void main(String[] args) throws ParseException {
        System.out.println(
                NetURI.toString(NetURI.parse("/evaluate/favicon.ico").subURI(1, 2))
        );
    }
}
