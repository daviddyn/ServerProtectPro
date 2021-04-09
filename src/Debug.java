import com.davidsoft.http.HttpURI;
import com.davidsoft.serverprotect.libs.HttpPath;
import com.davidsoft.utils.URI;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

public final class Debug {

    public static void main(String[] args) throws ParseException {
        HttpURI uri = HttpURI.parse("/Composing/Her Eyes/Her Eyers - Literary & Artistic Edition.mp3");
        System.out.println(uri);
    }
}
