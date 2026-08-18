package com.wellness.Service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyBandMessagePolicyTest {

    private final EnergyBandMessagePolicy policy = new EnergyBandMessagePolicy();

    @Test
    void scoresInTheSameBandUseExactlyTheSameMessage() {
        assertThat(policy.homeMessage(1)).isEqualTo(policy.homeMessage(20));
        assertThat(policy.homeMessage(21)).isEqualTo(policy.homeMessage(40));
        assertThat(policy.homeMessage(41)).isEqualTo(policy.homeMessage(60));
        assertThat(policy.homeMessage(61)).isEqualTo(policy.homeMessage(80));
        assertThat(policy.homeMessage(81)).isEqualTo(policy.homeMessage(100));

        assertThat(policy.routeMessage(1)).isEqualTo(policy.routeMessage(20));
        assertThat(policy.routeMessage(21)).isEqualTo(policy.routeMessage(40));
        assertThat(policy.routeMessage(41)).isEqualTo(policy.routeMessage(60));
        assertThat(policy.routeMessage(61)).isEqualTo(policy.routeMessage(80));
        assertThat(policy.routeMessage(81)).isEqualTo(policy.routeMessage(100));
    }

    @Test
    void adjacentBandsUseDifferentMessages() {
        assertThat(policy.homeMessage(20)).isNotEqualTo(policy.homeMessage(21));
        assertThat(policy.homeMessage(40)).isNotEqualTo(policy.homeMessage(41));
        assertThat(policy.homeMessage(60)).isNotEqualTo(policy.homeMessage(61));
        assertThat(policy.homeMessage(80)).isNotEqualTo(policy.homeMessage(81));
    }
}
