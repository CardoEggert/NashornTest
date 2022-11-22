import java.util.List;

public class SomeProvider {

    public long someRequest(Long first, Long second) {
        // Imitate a request
        try {
            Thread.sleep(2_000);
        } catch (Exception ignored) {
        }
        // Try to use stream here
        final List<Long> list = List.of(first, second);
        return list.stream().filter(x -> x > 0).filter(x -> x < 10).count();
    }
}
