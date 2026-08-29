package io.github.chenyilei2016.maintain.manager.utils;

import lombok.experimental.UtilityClass;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * @author chenyilei
 * @since 2025/07/31 22:03
 */
@UtilityClass
public class MyProfileUtils {


    public static boolean isProd(Environment environment) {
        return isProd(activeOrDefaultProfiles(environment));
    }

    public static boolean isPre(Environment environment) {
        return isPre(activeOrDefaultProfiles(environment));
    }

    public static boolean isTestTrunk(Environment environment) {
        return isTestTrunk(activeOrDefaultProfiles(environment));
    }

    public static boolean isLocal(Environment environment) {
        return isLocal(activeOrDefaultProfiles(environment));
    }

    public static boolean isUnit(Environment environment) {
        return isUnit(activeOrDefaultProfiles(environment));
    }

    public static boolean isProject(Environment environment) {
        return isProject(activeOrDefaultProfiles(environment));
    }

    public static boolean isProd(String[] activeProfiles) {
        return contains(activeProfiles, "prod");
    }

    public static boolean isPre(String[] activeProfiles) {
        return contains(activeProfiles, "pre");
    }

    public static boolean isTestTrunk(String[] activeProfiles) {
        return contains(activeProfiles, "test-trunk");
    }

    public static boolean isLocal(String[] activeProfiles) {
        return contains(activeProfiles, "local");
    }

    public static boolean isProject(String[] activeProfiles) {
        return contains(activeProfiles, "project") ||
                isStartWith(activeProfiles, "project-");
    }

    public static boolean isUnit(String[] activeProfiles) {
        return isStartWith(activeProfiles, "unit");
    }

    public static boolean contains(String[] activeProfiles, String profile) {
        return Arrays.stream(activeProfiles).anyMatch(profile::equalsIgnoreCase);
    }

    public static boolean isStartWith(String[] activeProfiles, String profile) {
        return Arrays.stream(activeProfiles)
                .anyMatch(activeProfile -> activeProfile.regionMatches(true, 0, profile, 0, profile.length()));
    }

    private static String[] activeOrDefaultProfiles(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length == 0 ? environment.getDefaultProfiles() : activeProfiles;
    }

}
