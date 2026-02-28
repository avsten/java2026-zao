import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PingChecker {

    // regex для строк вида: "64 bytes from ... time=12.3 ms"
    private static final Pattern TIME_PATTERN =
            Pattern.compile("time=([0-9.]+)\\s*ms");
    private static String commandWindows = "ping437.cmd -n 4 "; //строка для запуска ping из windows 
    //необходимо создать в папке запуска программы командный файл первая комманда chcp переключает кодовую страницу консоли на латиницу cp437:
    //командный файл получает 3 параметра третий это ip адрес или DNS имя хоста, доступность которого проверяем
    //chcp 437 
    //ping.exe %1 %2 %3
    //
    private static String commandLinux = "ping -c 4 ";                                      //строка для запуска ping из linux
    //метод запуска команды ping
    private static Double runPingCommand(String ip) {
        try {
            Process process;                                                                //переменная для идентификатора запускаемого процесса
            String os = System.getProperty("os.name").toLowerCase();                        //получаем имя операционной системы
            if (os.contains("win")) {                                                       //семейство windows
                //запускаем процесс ping из коммандной строки с параметрами 
                process = Runtime.getRuntime().exec(commandWindows+ip);                     //настроено на windowы
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {    //семейство linux
                //запускаем процесс ping из коммандной строки с параметрами 
                process = Runtime.getRuntime().exec(commandLinux+ip);                       //настроено на linux
            } else {
                System.out.println("Другая ОС: " + os);                                     //другой тип и мы не знаем параметров команды ping
                return null;
            }
            //создаем буфер ввода
            BufferedReader stdInput =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            //создаем буфер для ошибок
                    BufferedReader stdError =
                    new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;                                                                    //строка для чтения из буфера
            double sum = 0.0;                                                               //переменная под сумму для расчета среднего значения задержки
            int count = 0;                                                                  //переменная под количество образцов

            System.out.println("=== Вывод ping для " + ip + " ===");
            while ((line = stdInput.readLine()) != null) {
                System.out.println(line);

                // пытаемся вытащить time из строк
                Matcher m = TIME_PATTERN.matcher(line);                                     //разбираем строку по формату regexp
                if (m.find()) {
                    double t = Double.parseDouble(m.group(1));                              //преобразуем найденную подстроку со значением времени в double
                    sum += t;                                                               //накапливаем суммарную задержку за все операции (в нашем случае 20 ответов)
                    count++;                                                                //подсчитываем количество строк ответа соответствующих формату
                }
            }

            while ((line = stdError.readLine()) != null) {                                  //выводим строки из потока ошибок
                System.out.println(line);
            }

            int exitCode = process.waitFor();                                               //ожидаем завершения процесса и считываем код возврата
            if (exitCode != 0 || count == 0) {
                return null;                                                                // ошибка или не удалось вытащить времена (код возврата не 0 и количество найденных ответов по формату 0)
            }

            return sum / count;

        } catch (IOException | InterruptedException e) {
            System.out.println("Ошибка при выполнении ping для " + ip + ": " + e.getMessage());
            return null;
        }
    }
//метод main
    public static void main(String[] args) { 
        Scanner scanner = new Scanner(System.in);
        System.out.println("Введите IP-адреса для проверки (пустая строка — завершение):");

        while (true) {
            System.out.print("IP: ");
            String ip = scanner.nextLine().trim();                                          //убираем лишние пробелы

            if (ip.isEmpty()) {                                                             //проверяем что строка ввода пустая
                System.out.println("Завершение работы.");
                break;
            }
            //запускаем метод unPingCommand
            Double avgTime = runPingCommand(ip);
            if (avgTime == null) {
                System.out.println("Хост " + ip + " недоступен или не удалось получить время ответа.\n");
            } else {
                System.out.printf("Хост %s доступен, среднее время ответа: %.2f ms%n%n", ip, avgTime);
            }
        }
        scanner.close();
    }
}
