package com.reservations.model;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entité centrale représentant une réservation effectuée par un Utilisateur pour une Salle.
 * - Relation ManyToOne avec Utilisateur (obligatoire, clef étrangère 'utilisateur_id')
 * - Relation ManyToOne avec Salle (obligatoire, clef étrangère 'salle_id')
 */
@Entity
@Table(name = "reservations")
public class Reservation implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference", nullable = false, unique = true, length = 50)
    private String reference;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 30)
    private StatutReservation statut = StatutReservation.CONFIRMEE;

    @Column(name = "motif", length = 200)
    private String motif;

    /**
     * Plusieurs réservations appartiennent à un seul Utilisateur.
     * Côté propriétaire de la relation ManyToOne.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    /**
     * Plusieurs réservations peuvent concerner une Salle.
     * Côté propriétaire de la relation ManyToOne.
     * Cascade PERSIST et MERGE permettent de propager la sauvegarde de la salle si celle-ci est nouvelle.
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    public Reservation() {
        this.dateDebut = LocalDateTime.now();
        this.dateFin = this.dateDebut.plusHours(2);
        this.reference = "RES-" + System.currentTimeMillis();
    }

    public Reservation(String reference, LocalDateTime dateDebut, LocalDateTime dateFin, String motif) {
        this.reference = reference;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.motif = motif;
        this.statut = StatutReservation.CONFIRMEE;
    }

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public StatutReservation getStatut() {
        return statut;
    }

    public void setStatut(StatutReservation statut) {
        this.statut = statut;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Salle getSalle() {
        return salle;
    }

    public void setSalle(Salle salle) {
        this.salle = salle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        if (reference != null && that.reference != null) {
            return Objects.equals(reference, that.reference);
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return reference != null ? Objects.hash(reference) : (id != null ? Objects.hash(id) : 31);
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", reference='" + reference + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", statut=" + statut +
                ", motif='" + motif + '\'' +
                '}';
    }
}
