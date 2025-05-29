import ru.webbee.testtask.models.LogModel;
import static ru.webbee.testtask.utils.LogProcessor.processUserLogs;
import static ru.webbee.testtask.utils.LogProcessor.readAllLogs;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            List<LogModel> logs = readAllLogs("src/ru/webbee/testtask/logs");
            processUserLogs(logs, "src/ru/webbee/testtask/logs");
            System.out.println("Processing completed successfully!");
        } catch (IOException e) {
            System.err.println("Error processing logs: " + e.getMessage());
        }

    }
}