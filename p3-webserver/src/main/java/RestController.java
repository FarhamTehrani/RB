import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class RestController {
    public static String returnDate(){
        LocalDate currentDate = LocalDate.now();
        return currentDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    public static String returnTime(){
        LocalTime currentTime = LocalTime.now();
        return currentTime.format(DateTimeFormatter.ofPattern("hh:mm:ss"));
    }
}

