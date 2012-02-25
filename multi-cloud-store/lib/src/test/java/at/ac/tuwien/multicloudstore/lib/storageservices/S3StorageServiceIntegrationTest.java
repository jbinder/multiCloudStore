package at.ac.tuwien.multicloudstore.lib.storageservices;


import at.ac.tuwien.multicloudstore.lib.common.FileSystemNode;
import org.junit.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

public class S3StorageServiceIntegrationTest {

    private S3StorageService subject = null;
    private static String testDir = null;

    private static String bucket;
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

        bucket = prop.getProperty("s3_bucket");
        username = prop.getProperty("s3_username");
        password = prop.getProperty("s3_password");
        testFileNameLocal = prop.getProperty("s3_testFileName");
        testFileNameRemote = prop.getProperty("s3_remoteTestFileName");

        if (bucket == null || testFileNameLocal == null || testFileNameRemote == null || username == null || password == null ) {
            throw new ConfigFileException("A parameter is missing in your config.properties file." +
                    "See config.dist.properties for an example.");
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        subject = new S3StorageService();
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
            subject.init(bucket, username, password);
            subject.connect();
        } catch (StorageServiceUnreachableException e) {
            exception = true;
        }
        Assert.assertEquals(false, exception);
    }

    @Test(expected=StorageServiceUnreachableException.class)
    public void testConnectInvalidBucket() throws StorageServiceOperationException, StorageServiceUnreachableException {
        subject.init(bucket + "blah", username, password);
        subject.connect();
    }

    @Test(expected=StorageServiceUnreachableException.class)
    public void testConnectInvalidUser() throws StorageServiceOperationException, StorageServiceUnreachableException {
        subject.init(bucket, username + "blah", password);
        subject.connect();
    }

    @Test
    public void testUploadAndList() throws StorageServiceOperationException {
        connect();
        subject.upload(testFileNameLocal, remoteFileName);
        FileSystemNode node = subject.listAll().getChild(testDir);
        Assert.assertNotSame(node.getChild(testFileNameRemote), null);
        subject.delete(remoteFileName);
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
        Assert.assertSame(node, null);
    }

    private void connect() {
        try {
            subject.init(bucket, username, password);
            subject.connect();
        } catch (StorageServiceUnreachableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
