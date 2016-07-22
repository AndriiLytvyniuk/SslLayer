# SslLayer
SslLayer is a class that provides encryption/decryption for channel using SSL.

As an engine it uses SSLEngine.java.
Class performs all operations related to single SSL connection.
It requires:
    1) I/O with encrypted data from one side: (readEncryptedChannel and writeEncryptedChannel)
    2) I/O with decrypted data from other (decryptedInputStream and decryptedOutputStream)
    3) SSLEngine, client or server
It provides one session connection: handshake + data exchange.
After SslLayer is connected, just try to write or receive information from decrypted channels,
all handshake operations will be performed automatically. If handshake is not successful, SSLException will be thrown

Example of usage can be found in tests.
