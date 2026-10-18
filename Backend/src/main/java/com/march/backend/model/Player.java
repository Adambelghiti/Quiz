package com.march.backend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String avatar; // optional

    /**
     * Id of the {@link AuthUser} that owns this player. May be {@code null} for
     * anonymous/legacy players created before accounts were linked.
     * <p>
     * Read-only over JSON: it is still serialised in responses but must never be
     * supplied by a client, otherwise a caller could claim another user's
     * identity through the legacy player endpoints.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(unique = true)
    private Long authUserId;
}
