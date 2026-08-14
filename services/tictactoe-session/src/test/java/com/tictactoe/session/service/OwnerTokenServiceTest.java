package com.tictactoe.session.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OwnerTokenServiceTest {

    private final OwnerTokenService ownerTokenService = new OwnerTokenService();

    @Test
    void generateProducesDifferentTokensEachTime() {
        String first = ownerTokenService.generate();
        String second = ownerTokenService.generate();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void hashIsSixtyFourLowercaseHexCharsAndStableForTheSameInput() {
        String hash = ownerTokenService.hash("some-token");

        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
        assertThat(ownerTokenService.hash("some-token")).isEqualTo(hash);
    }

    @Test
    void matchesIsTrueForTheRightTokenAndFalseForANearMiss() {
        String token = ownerTokenService.generate();
        String hash = ownerTokenService.hash(token);

        assertThat(ownerTokenService.matches(token, hash)).isTrue();
        assertThat(ownerTokenService.matches(token + "x", hash)).isFalse();
        assertThat(ownerTokenService.matches(null, hash)).isFalse();
        assertThat(ownerTokenService.matches(token, null)).isFalse();
    }
}
