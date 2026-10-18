package com.march.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "players")
@Data
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
     */
    @Column(unique = true)
    private Long authUserId;
}
