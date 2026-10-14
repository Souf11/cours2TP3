package com.reservations;

import com.reservations.model.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Application principale démontrant les relations JPA, les cascades et le cycle de vie des entités.
 */
public class App {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("        DÉMARRAGE DU TP : GESTION DES RÉSERVATIONS AVEC JPA / HIBERNATE         ");
        System.out.println("================================================================================");

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion-reservations-pu");
        EntityManager em = emf.createEntityManager();

        Long aliceId = null;
        Long res1Id = null;
        Long res2Id = null;
        Long salleTuringId = null;
        Long projecteurId = null;

        try {
            // =========================================================================
            // SCÉNARIO 1 : Persistance en cascade (CascadeType.ALL / PERSIST)
            // =========================================================================
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(">>> SCÉNARIO 1 : Création et persistance en cascade");
            System.out.println("--------------------------------------------------------------------------------");

            em.getTransaction().begin();

            // 1. Création de l'utilisateur
            Utilisateur alice = new Utilisateur("alice.dupont@entreprise.fr", "Dupont", "Alice", "R&D - Ingénierie");

            // 2. Création d'une salle et de ses équipements
            Salle salleTuring = new Salle("SALLE-A101", "Salle Alan Turing", 25, 1);

            Equipement projecteur = new Equipement("EQ-VP-01", "Vidéoprojecteur Laser 4K", "Projecteur haute définition avec HDMI & Wi-Fi");
            Equipement tableau = new Equipement("EQ-TBI-02", "Tableau Blanc Interactif", "Écran tactile interactif 75 pouces");

            // Association ManyToMany Salle <-> Equipement
            salleTuring.addEquipement(projecteur);
            salleTuring.addEquipement(tableau);

            // 3. Création des réservations
            Reservation res1 = new Reservation(
                    "RES-2026-001",
                    LocalDateTime.now().plusDays(1).withHour(9).withMinute(0),
                    LocalDateTime.now().plusDays(1).withHour(11).withMinute(0),
                    "Atelier Architecture Microservices"
            );
            res1.setSalle(salleTuring);

            Reservation res2 = new Reservation(
                    "RES-2026-002",
                    LocalDateTime.now().plusDays(2).withHour(14).withMinute(0),
                    LocalDateTime.now().plusDays(2).withHour(16).withMinute(0),
                    "Revue de Sprint & Rétrospective"
            );
            res2.setSalle(salleTuring);

            // Association OneToMany Utilisateur <-> Reservation via méthode utilitaire
            alice.addReservation(res1);
            alice.addReservation(res2);

            // Démonstration de la cascade :
            // Persister alice propage automatiquement la persistance vers res1 et res2 (CascadeType.ALL)
            // Et persister res1/res2 propage vers salleTuring puis vers projecteur/tableau (CascadeType.PERSIST)
            System.out.println("[INFO] Appel de em.persist(alice) -> propagation cascade en cours...");
            em.persist(alice);

            em.getTransaction().commit();

            aliceId = alice.getId();
            res1Id = res1.getId();
            res2Id = res2.getId();
            salleTuringId = salleTuring.getId();
            projecteurId = projecteur.getId();

            System.out.println("[SUCCÈS] Utilisateur persisté avec ID : " + aliceId);
            System.out.println("[SUCCÈS] Réservation 1 ID : " + res1Id + ", Réservation 2 ID : " + res2Id);
            System.out.println("[SUCCÈS] Salle Turing ID : " + salleTuringId);
            System.out.println("[SUCCÈS] Équipement projecteur ID : " + projecteurId);

            // =========================================================================
            // SCÉNARIO 2 : Relation ManyToMany - Partage d'un équipement entre plusieurs salles
            // =========================================================================
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(">>> SCÉNARIO 2 : Partage d'équipement entre salles (ManyToMany sans duplication)");
            System.out.println("--------------------------------------------------------------------------------");

            em.getTransaction().begin();

            Salle salleLovelace = new Salle("SALLE-B202", "Salle Ada Lovelace", 15, 2);
            // Réutilisation de l'équipement déjà existant
            Equipement existingProjector = em.find(Equipement.class, projecteurId);
            salleLovelace.addEquipement(existingProjector);

            em.persist(salleLovelace);
            em.getTransaction().commit();

            System.out.println("[SUCCÈS] Salle Lovelace créée et associée à l'équipement existant : " + existingProjector.getNom());

            // =========================================================================
            // SCÉNARIO 3 : Test de suppression orpheline (orphanRemoval = true)
            // =========================================================================
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(">>> SCÉNARIO 3 : Test de orphanRemoval = true sur les réservations");
            System.out.println("--------------------------------------------------------------------------------");

            em.getTransaction().begin();

            Utilisateur userToModify = em.find(Utilisateur.class, aliceId);
            System.out.println("[INFO] Réservations actuelles d'Alice : " + userToModify.getReservations().size());

            // Recherche de la réservation res1 dans la collection
            final Long targetRes1Id = res1Id;
            Reservation reservationToRemove = userToModify.getReservations().stream()
                    .filter(r -> r.getId().equals(targetRes1Id))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Réservation introuvable"));

            // Retrait de la collection via la méthode utilitaire
            userToModify.removeReservation(reservationToRemove);

            System.out.println("[INFO] Réservation " + res1Id + " retirée du Set. Commit de la transaction...");
            // Le commit déclenche la requête SQL DELETE grâce à orphanRemoval = true
            em.getTransaction().commit();

            // Vérification en BDD
            Reservation checkRemoved = em.find(Reservation.class, res1Id);
            System.out.println("[VÉRIFICATION] La réservation " + res1Id + " est-elle présente en BDD ? " + (checkRemoved != null ? "OUI (ERREUR)" : "NON (SUPPRIMÉE CORRECTEMENT)"));

            // =========================================================================
            // SCÉNARIO 4 : Suppression en cascade d'un utilisateur (CascadeType.ALL)
            // =========================================================================
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(">>> SCÉNARIO 4 : Suppression d'un utilisateur et impact cascade sur ses réservations");
            System.out.println("--------------------------------------------------------------------------------");

            // Création d'un second utilisateur avec sa propre réservation
            em.getTransaction().begin();
            Utilisateur bob = new Utilisateur("bob.martin@entreprise.fr", "Martin", "Bob", "Marketing");
            Salle salleForBob = em.find(Salle.class, salleTuringId);

            Reservation bobRes = new Reservation(
                    "RES-2026-099",
                    LocalDateTime.now().plusDays(3).withHour(10).withMinute(0),
                    LocalDateTime.now().plusDays(3).withHour(12).withMinute(0),
                    "Présentation Stratégie Marketing"
            );
            bobRes.setSalle(salleForBob);
            bob.addReservation(bobRes);

            em.persist(bob);
            em.getTransaction().commit();

            Long bobId = bob.getId();
            Long bobResId = bobRes.getId();
            System.out.println("[INFO] Utilisateur Bob créé (ID: " + bobId + ") avec réservation (ID: " + bobResId + ")");

            // Suppression de Bob
            em.getTransaction().begin();
            Utilisateur bobToDelete = em.find(Utilisateur.class, bobId);
            System.out.println("[INFO] Suppression de Bob via em.remove()...");
            em.remove(bobToDelete);
            em.getTransaction().commit();

            // Vérifications :
            Reservation checkBobRes = em.find(Reservation.class, bobResId);
            Salle checkSalleStillExists = em.find(Salle.class, salleTuringId);

            System.out.println("[VÉRIFICATION] Réservation de Bob supprimée par cascade ? " + (checkBobRes == null ? "OUI (CONFORME)" : "NON (ERREUR)"));
            System.out.println("[VÉRIFICATION] La Salle Turing existe-t-elle toujours ? " + (checkSalleStillExists != null ? "OUI (NON AFFECTÉE, CONFORME)" : "NON (ERREUR CRITIQUE : SALLE SUPPRIMÉE INVOLONTAIREMENT)"));

            // =========================================================================
            // SCÉNARIO 5 : Requête JPQL avec JOIN FETCH (Évite le problème N+1)
            // =========================================================================
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println(">>> SCÉNARIO 5 : Consultation optimisée via JPQL JOIN FETCH");
            System.out.println("--------------------------------------------------------------------------------");

            List<Reservation> resultList = em.createQuery(
                    "SELECT DISTINCT r FROM Reservation r " +
                            "JOIN FETCH r.utilisateur " +
                            "JOIN FETCH r.salle s " +
                            "LEFT JOIN FETCH s.equipements " +
                            "WHERE r.statut = :statut", Reservation.class)
                    .setParameter("statut", StatutReservation.CONFIRMEE)
                    .getResultList();

            // En Hibernate 5, le JOIN FETCH sur une collection produit des doublons dans la liste JDBC
            // L'usage de LinkedHashSet permet de conserver l'ordre tout en garantissant l'unicité des entités racines
            java.util.Set<Reservation> activeReservations = new java.util.LinkedHashSet<>(resultList);

            System.out.println("[RÉSULTAT] Nombre de réservations actives distinctes trouvées : " + activeReservations.size());
            for (Reservation r : activeReservations) {
                System.out.println(" - " + r.getReference() + " : " + r.getMotif() +
                        " | Réservé par: " + r.getUtilisateur().getNom() + " " + r.getUtilisateur().getPrenom() +
                        " | Salle: " + r.getSalle().getNom() + " (Équipements: " + r.getSalle().getEquipements().size() + ")");
            }

        } finally {
            em.close();
            emf.close();
            System.out.println("\n================================================================================");
            System.out.println("        FIN DES TESTS DU TP - RESSOURCES FERMÉES AVEC SUCCÈS                    ");
            System.out.println("================================================================================");
        }
    }
}
