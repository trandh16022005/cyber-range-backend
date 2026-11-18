package com.cyberrange.backend.team.entity;

import java.io.Serializable;
import java.util.Objects;

public class TeamMemberId implements Serializable {

    private Long team;
    private Long user;

    public TeamMemberId() {
    }

    public TeamMemberId(Long team, Long user) {
        this.team = team;
        this.user = user;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof TeamMemberId that)) {
            return false;
        }

        return Objects.equals(team, that.team)
                && Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(team, user);
    }
}

