package com.reservations.model;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Entité représentant un équipement pouvant être partagé entre plusieurs salles.
 * Relation ManyToMany avec l'entité Salle.
 */
@Entity
@Table(name = "equipements")
public class Equipement implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code_inventaire", unique = true, length = 50)
    private String codeInventaire;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "description", length = 255)
    private String description;

    /**
     * Côté inverse (non-propriétaire) de la relation ManyToMany.
     * Mappé par le champ 'equipements' de l'entité Salle.
     * REMARQUE IMPORTANTE : Pas de CascadeType.REMOVE ici pour éviter qu'une
     * suppression d'équipement n'entraîne la suppression involontaire des salles associées !
     */
    @ManyToMany(mappedBy = "equipements")
    private Set<Salle> salles = new HashSet<>();

    // Constructeur sans argument obligatoire pour JPA
    public Equipement() {
    }

    public Equipement(String nom) {
        this.nom = nom;
    }

    public Equipement(String codeInventaire, String nom, String description) {
        this.codeInventaire = codeInventaire;
        this.nom = nom;
        this.description = description;
    }

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodeInventaire() {
        return codeInventaire;
    }

    public void setCodeInventaire(String codeInventaire) {
        this.codeInventaire = codeInventaire;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Salle> getSalles() {
        return salles;
    }

    public void setSalles(Set<Salle> salles) {
        this.salles = salles;
    }

    // Méthodes equals et hashCode basées sur la clé métier ou nom
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Equipement that = (Equipement) o;
        if (codeInventaire != null && that.codeInventaire != null) {
            return Objects.equals(codeInventaire, that.codeInventaire);
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return codeInventaire != null ? Objects.hash(codeInventaire) : (id != null ? Objects.hash(id) : 31);
    }

    @Override
    public String toString() {
        return "Equipement{" +
                "id=" + id +
                ", codeInventaire='" + codeInventaire + '\'' +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
