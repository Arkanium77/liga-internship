package ru.liga;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

public class AppTest {
    @Test
    public void assertmaster() {
        Assertions.assertThat(1 + 1).isEqualTo(2);
    }
}
