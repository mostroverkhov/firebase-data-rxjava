package com.github.mostroverkhov.firebase_rx_data.setup;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by Maksym Ostroverkhov on 15.02.2017.
 */

public class PropsCredentialsFactory implements CredentialsFactory {

    private final String propFile;

    public PropsCredentialsFactory(String propFile) {
        this.propFile = propFile;
    }

    @Override
    public Credentials getCreds() {

        Properties props = new Properties();
        InputStream propsStream = getClass()
                .getClassLoader().getResourceAsStream(propFile);
        if (propsStream == null) {
            throw new IllegalArgumentException("Cant find property file: " + propFile);
        }

        try {
            props.load(propsStream);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error while loading property file: " + propFile);
        }
        String serviceFile = props.getProperty("authFile");
        String dbUrl = props.getProperty("dbUrl");
        String dbUserId = props.getProperty("dbUserId");

        String invalidMsg = validate(propFile,
                new Prop(serviceFile, "authFile"),
                new Prop(dbUrl, "dbUrl"),
                new Prop(dbUserId, "dbUserId"));
        if (invalidMsg.isEmpty()) {
            return new Credentials(dbUrl, dbUserId, serviceFile);
        } else {
            throw new IllegalArgumentException(invalidMsg);
        }
    }

    private static String validate(String propFile, Prop... props) {
        List<String> emptyProps = new ArrayList<>();
        for (Prop prop : props) {
            if (isEmpty(prop.getValue())) {
                emptyProps.add(prop.getName());
            }
        }
        return buildErrorMsg(propFile, emptyProps);
    }

    static String buildErrorMsg(String propFile, List<String> emptyProps) {
        if (emptyProps.isEmpty()) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Property file ").append(propFile).append(" lacks required properties: ");

            for (String emptyProp : emptyProps) {
                sb.append(emptyProp).append(", ");
            }
            return sb.toString();
        }
    }

    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    private static class Prop {
        private String value;
        private String name;

        public Prop(String value, String name) {
            this.value = value;
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public String getName() {
            return name;
        }
    }

}
