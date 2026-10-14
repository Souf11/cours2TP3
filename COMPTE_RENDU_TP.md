# Compte Rendu de TP : Relations JPA Avancées, Stratégies de Cascade et Gestion des Orphelins

**Projet :** Gestion de Réservations de Salles et d'Équipements  
**Technologie :** Java 17+, Maven, JPA 2.2, Hibernate 5.6, H2 Database (in-memory), JUnit 5  

---

## 1. Vue d'Ensemble & Spécificités de l'Implémentation

Ce TP a pour objectif de concevoir, implémenter et tester une architecture de persistance objet-relationnelle (ORM) avec JPA et Hibernate. Le cas d'usage modélise un système de réservation d'espaces professionnels (salles de réunion, studios, amphithéâtres) dotés d'équipements partagés.

### Schéma Conceptuel des Entités et Relations

```mermaid
classDiagram
    direction LR

    class Utilisateur {
        +Long id
        +String email [unique]
        +String nom
        +String prenom
        +String departement
        +Set~Reservation~ reservations
        +addReservation(Reservation)
        +removeReservation(Reservation)
    }

    class Reservation {
        +Long id
        +String reference [unique]
        +LocalDateTime dateDebut
        +LocalDateTime dateFin
        +StatutReservation statut
        +String motif
        +Utilisateur utilisateur
        +Salle salle
    }

    class Salle {
        +Long id
        +String code [unique]
        +String nom
        +int capacite
        +int etage
        +Set~Reservation~ reservations
        +Set~Equipement~ equipements
        +addEquipement(Equipement)
        +removeEquipement(Equipement)
    }

    class Equipement {
        +Long id
        +String codeInventaire [unique]
        +String nom
        +String description
        +Set~Salle~ salles
    }

    Utilisateur "1" --> "0..*" Reservation : OneToMany\n(CascadeType.ALL, orphanRemoval=true)
    Reservation "0..*" --> "1" Utilisateur : ManyToOne\n(JoinColumn: utilisateur_id)
    Reservation "0..*" --> "1" Salle : ManyToOne\n(JoinColumn: salle_id)
    Salle "1" --> "0..*" Reservation : OneToMany\n(mappedBy = salle)
    Salle "0..*" <--> "0..*" Equipement : ManyToMany\n(JoinTable: salle_equipements)
```

---

## 2. Définition et Analyse des Relations

### 2.1. Relation `@OneToMany` / `@ManyToOne` : `Utilisateur` $\leftrightarrow$ `Reservation`

- **Côté propriétaire (`Reservation`) :**  
  Porte la clé étrangère physique `utilisateur_id` en base :
  ```java
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "utilisateur_id", nullable = false)
  private Utilisateur utilisateur;
  ```
- **Côté inverse (`Utilisateur`) :**  
  Définit la collection avec les options de cascade et de cycle de vie :
  ```java
  @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<Reservation> reservations = new HashSet<>();
  ```
- **Méthodes de synchronisation bidirectionnelle :**
  Pour éviter tout désalignement entre la mémoire Java et la persistance JPA :
  ```java
  public void addReservation(Reservation reservation) {
      this.reservations.add(reservation);
      reservation.setUtilisateur(this);
  }

  public void removeReservation(Reservation reservation) {
      this.reservations.remove(reservation);
      reservation.setUtilisateur(null);
  }
  ```

---

### 2.2. Relation `@ManyToMany` : `Salle` $\leftrightarrow$ `Equipement`

- **Côté propriétaire (`Salle`) :**  
  Déclare explicitement la table de jointure `salle_equipements` :
  ```java
  @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = "salle_equipements",
      joinColumns = @JoinColumn(name = "salle_id"),
      inverseJoinColumns = @JoinColumn(name = "equipement_id")
  )
  private Set<Equipement> equipements = new HashSet<>();
  ```
- **Côté non-propriétaire (`Equipement`) :**  
  Indique le miroir via `mappedBy` :
  ```java
  @ManyToMany(mappedBy = "equipements")
  private Set<Salle> salles = new HashSet<>();
  ```
- **Règle de sécurité critique :**  
  Il est **strictement déconseillé d'appliquer `CascadeType.REMOVE`** sur une relation `@ManyToMany`. Si `REMOVE` était activé sur `Salle`, supprimer une salle détruirait en cascade un vidéoprojecteur ou un micro partagé par d'autres salles !

---

## 3. Stratégies de Cascade et Suppression Orpheline

### 3.1. Tableau Comparatif : `CascadeType.REMOVE` vs `orphanRemoval = true`

| Critère | `CascadeType.REMOVE` | `orphanRemoval = true` |
| :--- | :--- | :--- |
| **Déclenchement** | Lors de `em.remove(parent)` | Lors du retrait de l'enfant d'une collection (`parent.getChildren().remove(child)`) OU lors de la suppression du parent |
| **Objectif** | Éliminer les enfants quand le parent meurt | Éliminer les entités qui perdent leur parent d'ancrage (empêche les données mortes en BDD) |
| **Applicabilité** | `@OneToOne`, `@OneToMany`, `@ManyToOne` | `@OneToOne`, `@OneToMany` uniquement |
| **Exemple dans le TP** | Supprimer `Bob` $\rightarrow$ supprime sa réservation en base | Supprimer `res1` du Set `user.getReservations()` $\rightarrow$ génère un `DELETE FROM reservations WHERE id = ?` |

---

## 4. Analyse Pas-à-Pas des Requêtes SQL Capturées

Voici les requêtes SQL réelles exécutées par Hibernate sur le moteur H2 pendant les 5 scénarios du programme principal (`App.java`) :

### Scénario 1 : Création et Cascade Persist
1. **Insertion de l'utilisateur :**
   ```sql
   INSERT INTO utilisateurs (id, departement, email, nom, prenom) VALUES (default, ?, ?, ?, ?);
   ```
2. **Insertion de la salle et des équipements :**
   ```sql
   INSERT INTO salles (id, capacite, code, etage, nom) VALUES (default, ?, ?, ?, ?);
   INSERT INTO equipements (id, code_inventaire, description, nom) VALUES (default, ?, ?, ?);
   ```
3. **Insertion des réservations (liées à l'utilisateur et à la salle) :**
   ```sql
   INSERT INTO reservations (id, date_debut, date_fin, motif, reference, salle_id, statut, utilisateur_id) 
   VALUES (default, ?, ?, ?, ?, ?, ?, ?);
   ```
4. **Insertion dans la table de jointure ManyToMany :**
   ```sql
   INSERT INTO salle_equipements (salle_id, equipement_id) VALUES (?, ?);
   ```

### Scénario 2 : Réutilisation sans Duplication (ManyToMany)
- Seule la nouvelle salle et le lien dans la table de jointure sont insérés ; l'équipement préexistant n'est pas recréé :
  ```sql
  INSERT INTO salles (id, capacite, code, etage, nom) VALUES (default, ?, ?, ?, ?);
  INSERT INTO salle_equipements (salle_id, equipement_id) VALUES (?, ?);
  ```

### Scénario 3 : Effet d'`orphanRemoval = true`
- Dès que `user.removeReservation(res)` est suivi de `em.getTransaction().commit()`, Hibernate constate que l'entité est orpheline :
  ```sql
  DELETE FROM reservations WHERE id = ?;
  ```

### Scénario 4 : Suppression d'un Utilisateur
- Grâce à `CascadeType.ALL` sur `Utilisateur.reservations` :
  ```sql
  DELETE FROM reservations WHERE id = ?;
  DELETE FROM utilisateurs WHERE id = ?;
  ```
- *Remarque :* La salle `salles` et les `equipements` ne subissent aucun `DELETE`.

### Scénario 5 : Résolution du Problème N+1 via JPQL `JOIN FETCH`
- Une seule requête SQL optimisée rapatrie la réservation, l'utilisateur, la salle et les équipements en un seul aller-retour :
  ```sql
  SELECT DISTINCT
      reservatio0_.id as id1_1_0_,
      utilisateu1_.id as id1_4_1_,
      salle2_.id as id1_3_2_,
      equipement4_.id as id1_0_3_,
      reservatio0_.date_debut as date_deb2_1_0_,
      ...
  FROM reservations reservatio0_
  INNER JOIN utilisateurs utilisateu1_ ON reservatio0_.utilisateur_id = utilisateu1_.id
  INNER JOIN salles salle2_ ON reservatio0_.salle_id = salle2_.id
  LEFT OUTER JOIN salle_equipements equipement3_ ON salle2_.id = equipement3_.salle_id
  LEFT OUTER JOIN equipements equipement4_ ON equipement3_.equipement_id = equipement4_.id
  WHERE reservatio0_.statut = ?;
  ```

---

## 5. Guide d'Exécution et Tests Automatisés

### Exécuter l'application principale :
```bash
mvn compile exec:java
```

### Exécuter les 4 tests unitaires JUnit 5 :
```bash
mvn test
```

Résultats constatés :
```text
[INFO] Running com.reservations.ReservationJpaTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.407 s -- in com.reservations.ReservationJpaTest
[INFO] BUILD SUCCESS
```

---

## 6. Synthèse des Bonnes Pratiques Appliquées

1. **Méthodes d'aide à la synchronisation :** Les méthodes `addReservation()`, `removeReservation()`, `addEquipement()` et `removeEquipement()` garantissent que les deux faces des relations en mémoire restent toujours synchronisées.
2. **Protection contre `StackOverflowError` :** Les méthodes `toString()`, `equals()` et `hashCode()` ont été soigneusement écrites pour ne jamais inspecter récursivement les collections associées.
3. **Imperméabilité des suppressions :** Pas de cascade `REMOVE` sur les relations partagées (`@ManyToMany` ou réservation $\rightarrow$ salle).
4. **Performance JPQL :** Élimination du problème du N+1 Select grâce au `JOIN FETCH`.
