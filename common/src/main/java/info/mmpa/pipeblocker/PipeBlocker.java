package info.mmpa.pipeblocker;

import info.mmpa.pipeblocker.logger.PipeBlockerJavaLogger;
import info.mmpa.pipeblocker.logger.PipeBlockerLog4jLogger;
import info.mmpa.pipeblocker.logger.PipeBlockerLogger;
import info.mmpa.pipeblocker.logger.PipeBlockerStdoutLogger;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

public class PipeBlocker {
    private static PipeBlockerLogger LOGGER = new PipeBlockerStdoutLogger();

    static {
        PipeBlocker.chooseBestLogger();
    }

    private static final List<Pattern> allowedPatterns = new ArrayList<>();
    private static final List<Pattern> rejectedPatterns = new ArrayList<>();
    private static final List<Pattern> softAllowedPatterns = new ArrayList<>();
    private static final HashMap<Class<?>, CheckStatus> cache = new HashMap<>();

    private static final Set<Class<?>> REJECTED_CLASSES = Collections.synchronizedSet(new HashSet<Class<?>>());

    private static int numEntriesLoaded = 0;

    private static String filterURL = "https://minecraft-malware-prevention-alliance.github.io/PipeBlocker/pipeblocker_filter.txt?t=" + new Date().getTime();
    private static boolean logOnly = Objects.equals(System.getProperty("info.mmpa.pipeblocker.log-only"), "true");
    private static FilterHook filterHook = null;
    private static boolean initialized = false;
    private static boolean allowUnsafe = false;
    private static boolean defaultAllow = false;

    private static void clearFilter() {
        numEntriesLoaded = 0;
        allowedPatterns.clear();
        rejectedPatterns.clear();
        softAllowedPatterns.clear();
        cache.clear();
    }

    private static void loadFilter() {
        // Attempt to load filter from GitHub
        try(InputStream filterStream = new URL(filterURL).openStream()) {
            processFilter(filterStream);
            LOGGER.info("Successfully loaded online PipeBlocker filter with " + numEntriesLoaded + " entries.");
        } catch(IOException e) {
            LOGGER.warn("Failed to load online filter, using local version: " + e);
            try(InputStream localFilterStream = PipeBlocker.class.getResourceAsStream("/pipeblocker_filter.txt")) {
                if(localFilterStream == null)
                    throw new FileNotFoundException("pipeblocker_filter.txt");
                processFilter(localFilterStream);
                LOGGER.info("Successfully loaded local PipeBlocker filter with " + numEntriesLoaded +  " entries.");
            } catch(IOException e2) {
                LOGGER.error("Failed to load local filter, this will mean no deserialization is permitted: " + e2);
            }
        }
        if (!allowUnsafe && (
                isMatchingName("info.mmpa.pipeblocker.test.UnsafeObject", allowedPatterns) ||
                isMatchingName("info.mmpa.pipeblocker.test.UnsafeObject", softAllowedPatterns) ||
                !isMatchingName("info.mmpa.pipeblocker.test.UnsafeObject", rejectedPatterns) ||
                defaultAllow
        )) {
            throw new RuntimeException("Unsafe PipeBlocker list detected -- please file an issue!");
        }
    }

    private static void processFilter(InputStream filterStream) throws IOException {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(filterStream))) {
            clearFilter();
            String line = reader.readLine();
            while(line != null) {
                processLine(line.trim());
                line = reader.readLine();
            }
        }
    }

    public static PipeBlockerLogger detectLogger() {
        try {
            Class.forName("org.apache.logging.log4j.Logger");
            Class.forName("org.apache.logging.log4j.LogManager");
            return new PipeBlockerLog4jLogger();
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class.forName("java.util.logging.Logger");
            return new PipeBlockerJavaLogger();
        } catch (ClassNotFoundException ignored) {
        }

        // Should never reach here
        return new PipeBlockerStdoutLogger();
    }

    public static void chooseBestLogger() {
        LOGGER = detectLogger();
    }

    public static void useStdOut() {
        LOGGER = new PipeBlockerStdoutLogger();
    }

    public static void useLog4j() {
        LOGGER = new PipeBlockerLog4jLogger();
    }

    public static void useJavaLogger() {
        LOGGER = new PipeBlockerJavaLogger();
    }

    private static void processLine(String line) {
        // ignore blank and comments
        if(line.isEmpty() || line.charAt(0) == '#')
            return;
        if (line.charAt(0) == '@') { // feature
            String feature = line.substring(1);
            if (feature.equals("default_allow")) {
                defaultAllow = true;
                LOGGER.debug("Default action set to allow.");
            } else {
                LOGGER.debug("Unknown feature '" + feature + "'.");
            }
            return;
        }
        // process glob lines
        String type = null;
        List<Pattern> list = null;
        switch (line.charAt(0)) {
            case '+': {
                type = "allow";
                list = allowedPatterns;
                break;
            }
            case '-': {
                type = "deny";
                list = rejectedPatterns;
                break;
            }
            case '~': {
                type = "soft allow";
                list = softAllowedPatterns;
                break;
            }
        }
        if (list != null) {
            String glob = line.substring(1);
            LOGGER.debug("Adding " + type + " rule for glob '" + glob + "'");
            Pattern desiredPattern = Pattern.compile(convertGlobToRegex(glob));
            list.add(desiredPattern);
            numEntriesLoaded++;
        }
    }

    private static Set<Class<?>> inheritanceStream(Class<?> clz) {
        if(clz == null)
            return Collections.emptySet();
        Set<Class<?>> streamBuilder = new HashSet<>();
        while(clz != null) {
            streamBuilder.add(clz);
            clz = clz.getSuperclass();
        }
        return streamBuilder;
    }

    private static boolean isMatchingName(String name, List<Pattern> patterns) {
        for(Pattern p : patterns) {
            if(p.matcher(name).matches()) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkPatterns(Class<?> clazz, List<Pattern> patterns) {
        Set<Class<?>> set = inheritanceStream(clazz);
        for (Class<?> c : set) if (PipeBlocker.isMatchingName(clazz.getCanonicalName(), patterns)) return true;
        return false;
    }

    private static FilterMatchType matchClass(Class<?> clazz) {
        if (checkPatterns(clazz, rejectedPatterns))
            return FilterMatchType.REJECT;
        if (checkPatterns(clazz, allowedPatterns))
            return FilterMatchType.ALLOW;
        if (PipeBlocker.isMatchingName(clazz.getCanonicalName(), softAllowedPatterns))
            return FilterMatchType.SOFT_ALLOW;
        if (defaultAllow)
            return FilterMatchType.DEFAULT_ALLOW;
        return FilterMatchType.DEFAULT;
    }

    public static CheckStatus check(Class<?> clazz) {
        if (clazz == null)
            return CheckStatus.UNDECIDED;

        if (cache.containsKey(clazz)) return cache.get(clazz);

        Class<?> underlyingClass = clazz;
        while (underlyingClass.isArray()) {
            underlyingClass = underlyingClass.getComponentType();
        }

        FilterMatchType matchType = matchClass(underlyingClass);

        if (matchType == FilterMatchType.SOFT_ALLOW || matchType == FilterMatchType.ALLOW || matchType == FilterMatchType.DEFAULT_ALLOW) {
            CheckStatus status = CheckStatus.UNDECIDED;
            if (filterHook != null) {
                status = filterHook.check(underlyingClass, matchType, status);
            }
            cache.put(clazz, status);
            return status;
        }

        CheckStatus status = logOnly?CheckStatus.UNDECIDED:CheckStatus.REJECTED;
        if (filterHook != null) {
            status = filterHook.check(underlyingClass, matchType, status);
        }

        if (filterHook != null && REJECTED_CLASSES.add(underlyingClass)) {
            LOGGER.warn("Blocked class " + underlyingClass.getName() + " from being deserialized as it's not allowed");
        }

        cache.put(clazz, status);

        return status;
    }

    public static void apply() {
        if (initialized) {
            throw new RuntimeException("PipeBlocker is already initialized!");
        }
        initialized = true;
        if (Objects.equals(System.getProperty("info.mmpa.pipeblocker.log-only"), "true")) {
            LOGGER.fatal("**************************************************************");
            LOGGER.fatal("*  WARNING: You are running PipeBlocker with log only mode.  *");
            LOGGER.fatal("*                                                            *");
            LOGGER.fatal("* This means the protections of PipeBlocker are disabled and *");
            LOGGER.fatal("* would be blocked deserialization attempts are only logged. *");
            LOGGER.fatal("* While this useful for figuring out what is broken, you     *");
            LOGGER.fatal("* SHOULD NOT use this mode while playing on multiplayer      *");
            LOGGER.fatal("* servers.                                                   *");
            LOGGER.fatal("*                                                            *");
            LOGGER.fatal("* You should disable this unless you are trying to figure    *");
            LOGGER.fatal("* out what mod is causing issues using the instructions on   *");
            LOGGER.fatal("* GitHub, or you were asked to on our issue tracker.         *");
            LOGGER.fatal("**************************************************************");
        }
        loadFilter();
        String javaVersion = System.getProperties().getProperty("java.specification.version");
        String className;
        if ("1.8".equals(javaVersion) || "1.7".equals(javaVersion)) {
            className = "info.mmpa.pipeblocker.java7.FilterSetter";
        } else {
            System.err.println("Unsupported java version: " + javaVersion);
            throw new RuntimeException("Unsupported java version: " + javaVersion);
        }
        try {
            Class.forName(className).getMethod("apply").invoke(null);
        } catch (ReflectiveOperationException ex) {
            System.out.println("Unable to invoke setter");
            throw new RuntimeException(ex);
        }
    }

    /**
     * Set the URL to load PipeBlocker filters from.
     * Must be executed before initialization.
     * @param url The URL to load the filter from.
     *            You are responsible for adding a cachebuster.
     * @throws RuntimeException Method is executed after initialization
     * */

    public static void setFilterURL(String url) {
        if (initialized) {
            throw new RuntimeException("Filter URL must be set before initialization");
        }
        filterURL = url;
    }

    /**
     * Sets the filter hook for PipeBlocker. This will be passed the class
     * attempted to be deserialized, the match type in the list and what
     * the decided action is, and the hook can decide to change the action
     * taken.
     * <p></p>
     * The hook can also be used to log all successful and unsuccessful
     * deserialization attempts.
     * <p></p>
     * Logging will also be disabled for failed deserialization.
     *
     * @param hook The filter hook to use.
     * @throws RuntimeException Method is executed after initialization
     * */

    public static void setFilterHook(FilterHook hook) {
        if (initialized) {
            throw new RuntimeException("Filter hook must be set before initialization");
        }
        filterHook = hook;
    }

    /**
     * Sets whether to only log blocked attempts, instead of actually blocking them.
     *
     * @param logOnly Whether to only log instead of blocking attempts.
     * @throws RuntimeException Method is executed after initialization
     * */

    public static void setLogOnly(boolean logOnly) {
        if (initialized) {
            throw new RuntimeException("Log-only status must be set before initialization");
        }
        PipeBlocker.logOnly = logOnly;
    }

    /**
     * Sets whether to allow unsafe lists.
     *
     * @param allowUnsafe Whether to allow unsafe lists.
     * @throws RuntimeException Method is executed after initialization
     * */

    public static void setAllowUnsafe(boolean allowUnsafe) {
        if (initialized) {
            throw new RuntimeException("Log-only status must be set before initialization");
        }
        PipeBlocker.allowUnsafe = allowUnsafe;
    }

    /**
     * Converts a standard POSIX Shell globbing pattern into a regular expression
     * pattern. The result can be used with the standard {@link java.util.regex} API to
     * recognize strings which match the glob pattern.
     * <p></p>
     * See also, the POSIX Shell language:
     * http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html#tag_02_13_01
     *
     * Code from https://stackoverflow.com/a/17369948, licensed under the public domain
     * @param pattern A glob pattern.
     * @return A regex pattern to recognize the given glob pattern.
     */
    public static String convertGlobToRegex(String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length() + 2);
        sb.append('^');
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
                case '\\':
                    if (++i >= arr.length) {
                        sb.append('\\');
                    } else {
                        char next = arr[i];
                        switch (next) {
                            case ',':
                                // escape not needed
                                break;
                            case 'Q':
                            case 'E':
                                // extra escape needed
                                sb.append('\\');
                            default:
                                sb.append('\\');
                        }
                        sb.append(next);
                    }
                    break;
                case '*':
                    if (inClass == 0)
                        sb.append(".*");
                    else
                        sb.append('*');
                    break;
                case '?':
                    if (inClass == 0)
                        sb.append('.');
                    else
                        sb.append('?');
                    break;
                case '[':
                    inClass++;
                    firstIndexInClass = i+1;
                    sb.append('[');
                    break;
                case ']':
                    inClass--;
                    sb.append(']');
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
                        sb.append('\\');
                    sb.append(ch);
                    break;
                case '!':
                    if (firstIndexInClass == i)
                        sb.append('^');
                    else
                        sb.append('!');
                    break;
                case '{':
                    inGroup++;
                    sb.append('(');
                    break;
                case '}':
                    inGroup--;
                    sb.append(')');
                    break;
                case ',':
                    if (inGroup > 0)
                        sb.append('|');
                    else
                        sb.append(',');
                    break;
                default:
                    sb.append(ch);
            }
        }
        sb.append('$');
        return sb.toString();
    }
}
