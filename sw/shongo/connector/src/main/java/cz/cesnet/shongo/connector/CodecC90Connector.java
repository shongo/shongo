package cz.cesnet.shongo.connector;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.ConnectorInfo;
import cz.cesnet.shongo.connector.api.ConnectorInitException;
import cz.cesnet.shongo.connector.api.DeviceInfo;
import cz.cesnet.shongo.connector.api.EndpointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A connector for Cisco TelePresence System Codec C90.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CodecC90Connector extends AbstractConnector implements EndpointService
{
    private static Logger logger = LoggerFactory.getLogger(CodecC90Connector.class);

    public static final int MICROPHONES_COUNT = 8;

    /**
     * An example of interaction with the device.
     *
     * Just for debugging purposes.
     *
     * @param args
     * @throws IOException
     * @throws CommandException
     * @throws InterruptedException
     * @throws ConnectorInitException
     * @throws SAXException
     * @throws XPathExpressionException
     */
    public static void main(String[] args)
            throws IOException, CommandException, InterruptedException, ConnectorInitException, SAXException,
                   XPathExpressionException, TransformerException, ParserConfigurationException
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        final String address;
        final String username;
        final String password;

        if (args.length > 0) {
            address = args[0];
        }
        else {
            System.out.print("address: ");
            address = in.readLine();
        }

        if (args.length > 1) {
            username = args[1];
        }
        else {
            System.out.print("username: ");
            username = in.readLine();
        }

        if (args.length > 2) {
            password = args[2];
        }
        else {
            System.out.print("password: ");
            password = in.readLine();
        }

        final CodecC90Connector conn = new CodecC90Connector();
        conn.connect(Address.parseAddress(address), username, password);

        Document result = conn.exec(new Command("xstatus SystemUnit uptime"));
        System.out.println("result:");
        printDocument(result, System.out);
        if (conn.isError(result)) {
            System.err.println("Error: " + conn.getErrorMessage(result));
            System.exit(1);
        }
        System.out.println("Uptime: " + getResultString(result, "/XmlDoc/Status/SystemUnit/Uptime"));
        System.out.println();

        Document calls = conn.exec(new Command("xStatus Call"));
        System.out.println("calls:");
        printDocument(calls, System.out);
        boolean activeCalls = !getResultString(calls, "/XmlDoc/Status/*").isEmpty();
        if (activeCalls) {
            System.out.println("There are some active calls");
        }
        else {
            System.out.println("There is no active call at the moment");
        }
        System.out.println();

        System.out.println("All done, disconnecting");
        conn.disconnect();
    }

    /**
     * The default port number to connect to.
     */
    public static final int DEFAULT_PORT = 22;

    /**
     * Shell channel open to the device.
     */
    private ChannelShell channel;

    /**
     * A writer for commands to be passed though the SSH channel to the device. Should be flushed explicitly.
     */
    private OutputStreamWriter commandStreamWriter;

    /**
     * A stream for reading results of commands.
     * Should be handled carefully (especially, it should not be buffered), because reading may cause a deadlock when
     * trying to read more than expected.
     */
    private InputStream commandResultStream;

    private static DocumentBuilder resultBuilder = null;

    /**
     * Connects the connector to the managed device.
     *
     * @param address  device address to connect to
     * @param username username to use for authentication on the device
     * @param password password to use for authentication on the device
     */
    public void connect(Address address, String username, final String password)
            throws CommandException
    {
        if (address.getPort() == Address.DEFAULT_PORT) {
            address.setPort(DEFAULT_PORT);
        }

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, address.getHost(), address.getPort());
            session.setPassword(password);
            // disable key checking - otherwise, the host key must be present in ~/.ssh/known_hosts
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            channel = (ChannelShell) session.openChannel("shell");
            commandStreamWriter = new OutputStreamWriter(channel.getOutputStream());
            commandResultStream = channel.getInputStream();
            channel.connect(); // runs a separate thread for handling the streams

            info.setConnectionState(ConnectorInfo.ConnectionState.CONNECTED);
            info.setDeviceAddress(address);

            initSession();
            initDeviceInfo();
        }
        catch (JSchException e) {
            throw new CommandException("Error in communication with the device", e);
        }
        catch (IOException e) {
            throw new CommandException("Error connecting to the device", e);
        }
        catch (SAXException e) {
            throw new CommandException("Command gave unexpected output", e);
        }
        catch (XPathExpressionException e) {
            throw new CommandException("Error querying command output XML tree", e);
        }
        catch (ParserConfigurationException e) {
            throw new CommandException("Error initializing result parser", e);
        }
    }

    private void initSession() throws IOException
    {
        // read the welcome message
        readOutput();

        sendCommand(new Command("echo off"));
        // read the result of the 'echo off' command
        readOutput();

        sendCommand(new Command("xpreferences outputmode xml"));
    }

    private void initDeviceInfo()
            throws IOException, SAXException, XPathExpressionException, ParserConfigurationException
    {
        Document result = exec(new Command("xstatus SystemUnit"));
        DeviceInfo di = new DeviceInfo();

        di.setName(getResultString(result, "/XmlDoc/Status/SystemUnit/ProductId"));
        di.setDescription(getResultString(result, "/XmlDoc/Status/SystemUnit/ContactInfo"));

        String version = getResultString(result, "/XmlDoc/Status/SystemUnit/Software/Version")
                + " (released "
                + getResultString(result, "/XmlDoc/Status/SystemUnit/Software/ReleaseDate")
                + ")";
        di.setSoftwareVersion(version);

        String sn = "Module: " + getResultString(result, "/XmlDoc/Status/SystemUnit/Hardware/Module/SerialNumber")
                + ", MainBoard: " + getResultString(result, "/XmlDoc/Status/SystemUnit/Hardware/MainBoard/SerialNumber")
                + ", VideoBoard: " + getResultString(result,
                "/XmlDoc/Status/SystemUnit/Hardware/VideoBoard/SerialNumber")
                + ", AudioBoard: " + getResultString(result,
                "/XmlDoc/Status/SystemUnit/Hardware/AudioBoard/SerialNumber");
        di.setSerialNumber(sn);

        info.setDeviceInfo(di);
    }

    /**
     * Disconnects the connector from the managed device.
     */
    public void disconnect() throws CommandException
    {
        Session session = null;
        if (channel != null) {
            try {
                session = channel.getSession();
            }
            catch (JSchException e) {
                throw new CommandException("Error disconnecting from the device", e);
            }
            channel.disconnect();
        }

        if (session != null) {
            session.disconnect();
        }
        commandStreamWriter = null; // just for sure the streams will not be used
        commandResultStream = null;

        info.setConnectionState(ConnectorInfo.ConnectionState.DISCONNECTED);
    }

    /**
     * Send a command to the device.
     * In case of an error, throws a CommandException with a detailed message.
     *
     * @param command command to be issued
     * @return the result of the command
     */
    private Document issueCommand(Command command) throws CommandException
    {
        logger.info(String.format("%s issuing command %s on %s", CodecC90Connector.class, command,
                info.getDeviceAddress()));

        try {
            Document result = exec(command);
            if (isError(result)) {
                logger.info(String.format("Command %s failed on %s: %s", command, info.getDeviceAddress(),
                        getErrorMessage(result)));
                throw new CommandException(getErrorMessage(result));
            }
            else {
                logger.info(String.format("Command %s succeeded on %s", command, info.getDeviceAddress()));
                return result;
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Command issuing error", e);
        }
        catch (SAXException e) {
            throw new RuntimeException("Command result parsing error", e);
        }
        catch (XPathExpressionException e) {
            throw new RuntimeException("Command result handling error", e);
        }
        catch (ParserConfigurationException e) {
            throw new RuntimeException("Error initializing result parser", e);
        }
    }


    /**
     * Sends a command to the device. Blocks until response to the command is complete.
     *
     * @param command a command to the device
     * @return output of the command
     * @throws IOException
     */
    private Document exec(Command command) throws IOException, SAXException, ParserConfigurationException
    {
        sendCommand(command);

        String output = readOutput();
        InputSource is = new InputSource(new StringReader(output));

        if (resultBuilder == null) {
            // lazy initialization
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            resultBuilder = factory.newDocumentBuilder();
        }

        return resultBuilder.parse(is);
    }

    private void sendCommand(Command command) throws IOException
    {
        if (info.getConnectionState() == ConnectorInfo.ConnectionState.DISCONNECTED) {
            throw new IllegalStateException("The connector is disconnected");
        }

        commandStreamWriter.write(command.toString() + '\n');
        commandStreamWriter.flush();
    }

    /**
     * Reads the output of the least recent unhandled command. Blocks until the output is complete.
     *
     * @return output of the least recent unhandled command
     * @throws IOException when the reading fails or end of the reading stream is met (which is not expected)
     */
    private String readOutput() throws IOException
    {
        if (commandResultStream == null) {
            throw new IllegalStateException("The connector is disconnected");
        }

        /**
         * Strings marking end of a command output.
         * Each must begin and end with "\r\n".
         */
        String[] endMarkers = new String[]{
                "\r\nOK\r\n",
                "\r\nERROR\r\n",
                "\r\n</XmlDoc>\r\n",
        };

        StringBuilder sb = new StringBuilder();
        int lastEndCheck = 0;
        int c;
reading:
        while ((c = commandResultStream.read()) != -1) {
            sb.append((char) c);
            if ((char) c == '\n') {
                // check for an output end marker
                for (String em : endMarkers) {
                    if (sb.indexOf(em, lastEndCheck) != -1) {
                        break reading;
                    }
                }
                // the next end marker check is needed only after this point
                lastEndCheck = sb.length() - 2; // one for the '\r' character, one for the end offset
            }
        }
        if (c == -1) {
            throw new IOException("Unexpected end of stream (was the connection closed?)");
        }
        return sb.toString();
    }


    private static XPathFactory xPathFactory = XPathFactory.newInstance();
    private static Map<String, XPathExpression> xPathExpressionCache = new HashMap<String, XPathExpression>();


    /**
     * Returns the result of an XPath expression on a given document. Caches the expressions for further usage.
     *
     * @param result      an XML document
     * @param xPathString an XPath expression
     * @return result of the XPath expression
     */
    private static String getResultString(Document result, String xPathString) throws XPathExpressionException
    {
        XPathExpression expr = xPathExpressionCache.get(xPathString);
        if (expr == null) {
            expr = xPathFactory.newXPath().compile(xPathString);
            xPathExpressionCache.put(xPathString, expr);
        }
        return expr.evaluate(result);
    }


    /**
     * Finds out whether a given result XML denotes an error.
     *
     * @param result an XML document - result of a command
     * @return true if the result marks an error, false if the result is an ordinary result record
     */
    private boolean isError(Document result) throws XPathExpressionException
    {
        String status = getResultString(result, "/XmlDoc/*[@status != '']/@status");
        return (status.contains("Error"));
    }


    /**
     * Given an XML result of an erroneous command, returns the error message.
     *
     * @param result an XML document - result of a command
     * @return error message contained in the result document, or null if the document does not denote an error
     */
    private String getErrorMessage(Document result) throws XPathExpressionException
    {
        if (!isError(result)) {
            return null;
        }

        String reason = getResultString(result, "/XmlDoc/Status[@status='Error']/Reason");
        String xPath = getResultString(result, "/XmlDoc/Status[@status='Error']/XPath");
        if (!reason.isEmpty() || !xPath.isEmpty()) {
            return reason + (xPath.isEmpty() ? "" : " (XPath: " + xPath + ")");
        }

        String description = getResultString(result, "/XmlDoc/*[@status='Error']/Description");
        if (!description.isEmpty()) {
            String cause = getResultString(result, "/XmlDoc/*[@status='Error']/Cause");
            return description + (cause.isEmpty() ? "" : String.format(" (Cause: %s)", cause));
        }

        String usage = getResultString(result, "/XmlDoc/*[@status='ParameterError']/../Usage");
        if (!usage.isEmpty()) {
            usage = usage.replace("&lt;", "<").replace("&gt;", ">");
            return "Parameter error. Usage: " + usage;
        }

        return "Uncategorized error";
    }


    // ENDPOINT SERVICE

    @Override
    public String dial(String address) throws CommandException
    {
        Command command = new Command("xCommand Dial");
        command.setParameter("Number", address);
        // NOTE: the BookingId parameter could be used to identify the reservation for which this dial is issued in call
        //       logs; other connectors are missing such a feature, however, so do we

        Document result = issueCommand(command);
        try {
            return getResultString(result, "/XmlDoc/DialResult/CallId");
        }
        catch (XPathExpressionException e) {
            throw new CommandException("Program error in parsing the command result.", e);
        }
    }

    @Override
    public String dial(Alias alias) throws CommandException
    {
        return dial(alias.getValue());
    }

    @Override
    public void hangUp(int callId) throws CommandException
    {
        Command command = new Command("xCommand Call Disconnect");
        command.setParameter("CallId", String.valueOf(callId));
        issueCommand(command);
    }

    @Override
    public void hangUpAll() throws CommandException
    {
        issueCommand(new Command("xCommand Call DisconnectAll"));
    }

    @Override
    public void resetDevice() throws CommandException
    {
        Command command = new Command("xCommand Boot");
        command.setParameter("Action", "Restart"); // should be default anyway, but just for sure...
        issueCommand(command);
    }

    @Override
    public void mute() throws CommandException
    {
        issueCommand(new Command("xCommand Audio Microphones Mute"));
    }

    @Override
    public void unmute() throws CommandException
    {
        issueCommand(new Command("xCommand Audio Microphones Unmute"));
    }

    @Override
    public void setMicrophoneLevel(int level) throws CommandException
    {
        // TODO: test that it really affects the microphones gain
        for (int i = 0; i < MICROPHONES_COUNT; i++) {
            Command cmd = new Command("xConfiguration Audio Input Microphone " + i);
            cmd.setParameter("Level", String.valueOf(level));
            issueCommand(cmd);
        }
    }

    @Override
    public void setPlaybackLevel(int level) throws CommandException
    {
        Command cmd = new Command("xConfiguration Audio");
        cmd.setParameter("Volume", String.valueOf(level));
        issueCommand(cmd);
    }

    @Override
    public void enableVideo() throws CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Enabling video is not supported on Codec C90.");
    }

    @Override
    public void disableVideo() throws CommandUnsupportedException
    {
        throw new CommandUnsupportedException("Disabling video is not supported on Codec C90.");
    }

    @Override
    public void startPresentation() throws CommandException
    {
        issueCommand(new Command("xCommand Presentation Start"));
    }

    @Override
    public void stopPresentation() throws CommandException
    {
        issueCommand(new Command("xCommand Presentation Stop"));
    }

    @Override
    public void standBy() throws CommandException
    {
        if (true /* TODO: should be "if there are some active calls" (maintain the device state) */) {
            // must hang up first for the command to work properly; otherwise, the command succeeds, but the device is
            //   not in the stand by state
            hangUpAll();

            // and we also must wait until all calls are really hung up; until then, the standby command has no effect
            final int attemptsLimit = 50;
            for (int i = 0; i < attemptsLimit; i++) {
                try {
                    Thread.sleep(100); // wait awhile; who knows for how long to be sure, though :-(
                }
                catch (InterruptedException e) {
                    // ignore - the calls are checked anyway and possibly sleeping again
                }
                Document calls = issueCommand(new Command("xStatus Call"));
                try {
                    if (getResultString(calls, "/XmlDoc/Status/*").isEmpty()) {
                        break;
                    }
                }
                catch (XPathExpressionException e) {
                    throw new CommandException("Program error in command execution.", e);
                }
            }
        }

        issueCommand(new Command("xCommand Standby Activate"));
    }
}
