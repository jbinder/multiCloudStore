package at.ac.tuwien.multicloudstore.lib.storageservices;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import at.ac.tuwien.multicloudstore.lib.common.FileSystemNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;

public class FtpStorageServiceIntegrationTest {

    private FtpStorageService subject = null;
    private static String testDir = null;

    private static String host;
    private static String username;
    private static String password;
    private static String testFileNameLocal;
    private static String testFileNameRemote;
    private static String remoteFileName;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream("lib/src/test/config.properties"));
        } catch (IOException e) {
            throw new ConfigFileException("Create a config.properties file in your test source folder. " +
                    "See config.dist.properties for an example.", e);
        }

        host = prop.getProperty("ftp_host");
        username = prop.getProperty("ftp_username");
        password = prop.getProperty("ftp_password");
        testFileNameLocal = prop.getProperty("ftp_testFileName");
        testFileNameRemote = prop.getProperty("ftp_remoteTestFileName");

        if (host == null || testFileNameLocal == null || testFileNameRemote == null) {
            throw new ConfigFileException("A parameter is missing in your config.properties file." +
                    "See config.dist.properties for an example.");
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        subject = new FtpStorageService();
        testDir = UUID.randomUUID().toString();
        remoteFileName = testDir + File.separator + testFileNameRemote;
    }

    @After
    public void tearDown() throws Exception {
        subject.disconnect();
    }

    @Test
    public void testConnect() {
        boolean exception = false;
        try {
            subject.init(host, username, password);
            subject.connect();
        } catch (StorageServiceUnreachableException e) {
            exception = true;
        }
        Assert.assertEquals(false, exception);
    }

    @Test(expected=StorageServiceUnreachableException.class)
    public void testConnectInvalidHost() throws StorageServiceOperationException, StorageServiceUnreachableException {
        subject.init(host + "blah", username, password);
        subject.connect();
    }

    @Test(expected=StorageServiceUnreachableException.class)
    public void testConnectInvalidUser() throws StorageServiceOperationException, StorageServiceUnreachableException {
        subject.init(host, username + "blah", password);
        subject.connect();
    }

    @Test
    public void testUploadAndList() throws StorageServiceOperationException {
        connect();
        subject.upload(testFileNameLocal, remoteFileName);
        FileSystemNode node = subject.listAll().getChild(testDir);
        Assert.assertNotSame(node.getChild(testFileNameRemote), null);
        subject.delete(remoteFileName);
        subject.delete(testDir);
    }

    @Test
    public void testDownload() throws StorageServiceOperationException {
        final String localFileName = testFileNameLocal + ".test";
        connect();
        subject.upload(testFileNameLocal, remoteFileName);
        subject.download(localFileName, remoteFileName);
        File localFile = new File(localFileName);
        Assert.assertTrue(localFile.exists());
        subject.delete(remoteFileName);
        subject.delete(testDir);
        Assert.assertTrue(localFile.delete());
    }

    @Test
    public void testDelete() throws StorageServiceOperationException {
        connect();
        subject.upload(testFileNameLocal, remoteFileName);
        FileSystemNode node = subject.listAll().getChild(testDir);
        Assert.assertNotSame(node.getChild(testFileNameRemote), null);
        subject.delete(remoteFileName);
        node = subject.listAll().getChild(testDir);
        Assert.assertSame(node.getChild(testFileNameRemote), null);
        subject.delete(testDir);
        Assert.assertSame(node.getChild(testDir), null);
    }

    private void connect() {
        try {
            subject.init(host, username, password);
            subject.connect();
        } catch (StorageServiceUnreachableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
