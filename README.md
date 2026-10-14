# TP JPA / Hibernate : Gestion Avancée des Réservations

Projet Maven complet démontrant la mise en œuvre de JPA (Java Persistence API) avec le provider Hibernate, une base en mémoire H2 et des tests unitaires JUnit 5.

## 🚀 Fonctionnalités Clés
- Relations complexes : `@OneToMany`, `@ManyToOne`, `@ManyToMany` avec table de jointure explicite.
- Stratégies de cascade (`CascadeType.ALL`, `CascadeType.PERSIST`, `CascadeType.MERGE`).
- Suppression des orphelins (`orphanRemoval = true`).
- Méthodes utilitaires défensives de synchronisation bidirectionnelle.
- Requête JPQL optimisée avec `JOIN FETCH` évitant le problème N+1.
- Suite de tests unitaires automatisés validant 100% des règles métier.

## 📁 Structure du Projet

```text
gestion-reservations/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/reservations/
    │   │   ├── App.java
    │   │   └── model/
    │   │       ├── Equipement.java
    │   │       ├── Reservation.java
    │   │       ├── Salle.java
    │   │       ├── StatutReservation.java
    │   │       └── Utilisateur.java
    │   └── resources/
    │       └── META-INF/
    │           └── persistence.xml
    └── test/
        └── java/com/reservations/
            └── ReservationJpaTest.java
```

## 🛠️ Commandes Utiles

### 1. Compiler et exécuter l'application principale (Démonstration des 5 scénarios) :
```bash
mvn compile exec:java
```

### 2. Exécuter tous les tests d'intégration JUnit 5 :
```bash
mvn test
```

### 3. Nettoyer le build :
```bash
mvn clean
```
