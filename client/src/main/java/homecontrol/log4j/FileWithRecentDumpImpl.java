package homecontrol.log4j;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.concurrent.locks.*;
import javax.swing.JOptionPane;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.layout.PatternLayout;
import ru.mikhailmineev.hallsensorapplication.NewJFrame;
import static ru.mikhailmineev.hallsensorapplication.NewJFrame.formatter;
import static ru.mikhailmineev.hallsensorapplication.NewJFrame.getApplicationPath;

// note: class name need not match the @Plugin name.
@Plugin(name = "FileWithRecentDump", category = "Core", elementType = "appender", printObject = true)
public final class FileWithRecentDumpImpl extends AbstractAppender {

    private static final CircularFifoQueue<String> recent = new CircularFifoQueue<>(20);

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();

    protected FileWithRecentDumpImpl(String name, Filter filter,
            Layout<? extends Serializable> layout, final boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions);
    }

    // The append method is where the appender does the work.
    // Given a log event, you are free to do with it what you want.
    // This example demonstrates:
    // 1. Concurrency: this method may be called by multiple threads concurrently
    // 2. How to use layouts
    // 3. Error handling
    @Override
    public void append(LogEvent event) {
        readLock.lock();
        try {
            byte[] ba = getLayout().toByteArray(event);
            String string = new String(ba, Charset.forName("UTF-8"));
            recent.add(string);

            if (event.getLevel().compareTo(Level.ERROR) <= 0) {
                Instant now = Instant.now();
                StringBuilder error = new StringBuilder();
                error.append("Error occured at ").append(formatter.format(now)).append('\n');
                //current error header
                error.append("Error message:").append('\n');
                error.append(string);
                //previos messages
                error.append("Last ").append(recent.size()).append(" messages:").append('\n');
                for (String e : recent) {
                    error.append(e);
                }

                //notify user about error
                JOptionPane.showMessageDialog(NewJFrame.getInstance(), "Неизвестная ошибка, смотри в файле!", "Ошибка", JOptionPane.ERROR_MESSAGE);

                File file = new File(getApplicationPath());
                String format = formatter.format(now);
                File file2 = new File(file.getParentFile(), NewJFrame.APPLICATION_NAME + " Exception (" + format + ").txt"
                );
                FileUtils.writeStringToFile(file2, error.toString());

                Level level = event.getLevel();
                Instant datetime = Instant.ofEpochMilli(event.getTimeMillis());
                String group = event.getLoggerName();
                String message = event.getMessage().getFormattedMessage();
            }
        } catch (Exception ex) {
            if (!ignoreExceptions()) {
                throw new AppenderLoggingException(ex);
            }
        } finally {
            readLock.unlock();
        }
    }

    // Your custom appender needs to declare a factory method
    // annotated with `@PluginFactory`. Log4j will parse the configuration
    // and call this factory method to construct an appender instance with
    // the configured attributes.
    @PluginFactory
    public static FileWithRecentDumpImpl createAppender(
            @PluginAttribute("name") String name,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("otherAttribute") String otherAttribute) {
        if (name == null) {
            LOGGER.error("No name provided for MyCustomAppenderImpl");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        return new FileWithRecentDumpImpl(name, filter, layout, true);
    }
}
