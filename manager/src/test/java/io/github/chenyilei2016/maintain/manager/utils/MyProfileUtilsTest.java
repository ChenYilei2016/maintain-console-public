package io.github.chenyilei2016.maintain.manager.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyProfileUtilsTest {

    @Test
    void usesDefaultProfilesOnlyWhenNoProfileIsActive() {
        MockEnvironment environment = new MockEnvironment();
        environment.setDefaultProfiles("local");

        assertTrue(MyProfileUtils.isLocal(environment));

        environment.setActiveProfiles("prod");
        assertFalse(MyProfileUtils.isLocal(environment));
        assertTrue(MyProfileUtils.isProd(environment));
    }
}
