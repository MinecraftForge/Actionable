package net.minecraftforge.actionable.util;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.kohsuke.github.GHApp;
import org.kohsuke.github.GHAppInstallationToken;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.authorization.AuthorizationProvider;

import java.io.IOException;
import java.security.PrivateKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

public class AuthUtil {
    private static final String PKCS1_KEY_START = "-----BEGIN RSA PRIVATE KEY-----\n";
    private static final String PKCS1_KEY_END = "-----END RSA PRIVATE KEY-----";
    private static final String PKCS8_KEY_START = "-----BEGIN PRIVATE KEY-----\n";
    private static final String PKCS8_KEY_END = "-----END PRIVATE KEY-----";

    public static byte[] parsePKCS8(String input) throws IOException {
        if (input.startsWith(PKCS8_KEY_START)) {
            input = input.replace(PKCS8_KEY_START, "").replace(PKCS8_KEY_END, "").replaceAll("\\s", "");
            return Base64.getDecoder().decode(input);
        } else {
            input = input.replace(PKCS1_KEY_START, "").replace(PKCS1_KEY_END, "").replaceAll("\\s", "");
            byte[] pkcs1Encoded = Base64.getDecoder().decode(input);
            AlgorithmIdentifier algId = new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE);
            PrivateKeyInfo privateKeyInfo = new PrivateKeyInfo(algId, ASN1Sequence.getInstance(pkcs1Encoded));
            return privateKeyInfo.getEncoded();
        }
    }

    public static AuthorizationProvider jwt(String appId, PrivateKey privateKey, TokenGetter tokenGetter) {
        return new AuthorizationProvider() {
            @Override
            public String getEncodedAuthorization() throws IOException {
                return "Bearer " + jwt();
            }

            private Jwt jwt = null;
            public String jwt() throws IOException {
                final Instant now = Instant.now();
                if (jwt == null) {
                    this.jwt = newJwt();
                } else if (now.isAfter(jwt.expirationDate())) {
                    this.jwt = newJwt();
                }
                return jwt.jwt();
            }

            public Jwt newJwt() throws IOException {
                final GitHub gitHub = new GitHubBuilder()
                        .withJwtToken(refreshJWT(appId, privateKey))
                        .build();

                final GHAppInstallationToken token = tokenGetter.getToken(gitHub.getApp());
                return new Jwt(token.getExpiresAt().toInstant(), token.getToken());
            }
        };
    }

    private static String refreshJWT(String appId, PrivateKey privateKey) {
        final Instant now = Instant.now();
        final Instant exp = now.plus(Duration.ofMinutes(10));
        final JwtBuilder builder = Jwts.builder()
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .setIssuer(appId)
                .signWith(privateKey, SignatureAlgorithm.RS256);
        return builder.compact();
    }

    public record Jwt(Instant expirationDate, String jwt) {}

    public interface TokenGetter {
        GHAppInstallationToken getToken(GHApp app) throws IOException;
    }
}
