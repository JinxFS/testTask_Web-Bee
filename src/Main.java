import ru.webbee.testtask.models.LogModel;
import static ru.webbee.testtask.utils.LogProcessor.processUserLogs;
import static ru.webbee.testtask.utils.LogProcessor.readAllLogs;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Введите абсолютный путь к директории с логами (Enter для дефолтного пути (src\\ru\\webbee\\testtask\\logs)  ): ");
        String input = scanner.nextLine().trim();

        String logsDir = input.isEmpty() ? "src/ru/webbee/testtask/logs" : input;

        try {
            List<LogModel> logs = readAllLogs(logsDir);
            processUserLogs(logs, logsDir);
            System.out.println("Обработка завершена успешно!");
        } catch (IOException e) {
            System.err.println("Ошибка при обработке логов: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }
}