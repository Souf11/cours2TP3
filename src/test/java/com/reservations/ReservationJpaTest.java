package com.reservations;

import com.reservations.model.*;
import org.junit.jupiter.api.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration automatisés pour valider les relations JPA,
 * les cascades et la suppression orpheline (orphanRemoval).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReservationJpaTest {

    private static EntityManagerFactory emf;
    private EntityManager em;

    @BeforeAll
    static void initFactory() {
        emf = Persistence.createEntityManagerFactory("gestion-reservations-pu");
    }

    @AfterAll
    static void closeFactory() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }

    @BeforeEach
    void setUp() {
        em = emf.createEntityManager();
    }

    @AfterEach
    void tearDown() {
        if (em != null && em.isOpen()) {
            em.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test 1 : Persistance en cascade (Utilisateur -> Réservations -> Salle -> Équipements)")
    void testCascadePersist() {
        em.getTransaction().begin();

        Utilisateur user = new Utilisateur("charlie.brown@ecole.fr", "Brown", "Charlie", "Étudiant");
        Salle salle = new Salle("SALLE-C303", "Salle Conférence C", 50, 3);
        Equipement micro = new Equipement("EQ-MIC-01", "Microphone Sans Fil", "Micro UHF professionnel");

        salle.addEquipement(micro);

        Reservation res = new Reservation("RES-TEST-001",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(2),
                "Soutenance de Projet");
        res.setSalle(salle);

        user.addReservation(res);

        // Persist propagé par cascade
        em.persist(user);
        em.getTransaction().commit();

        assertNotNull(user.getId(), "L'utilisateur doit posséder un identifiant généré");
        assertNotNull(res.getId(), "La réservation doit avoir été persistée par cascade");
        assertNotNull(salle.getId(), "La salle doit avoir été persistée par cascade");
        assertNotNull(micro.getId(), "L'équipement doit avoir été persisté par cascade");
    }

    @Test
    @Order(2)
    @DisplayName("Test 2 : orphanRemoval=true supprime l'entité orpheline en base")
    void testOrphanRemoval() {
        // Étape 1 : Créer un utilisateur avec 2 réservations
        em.getTransaction().begin();
        Utilisateur user = new Utilisateur("david.guetta@musique.fr", "Guetta", "David", "Son");
        Salle salle = new Salle("SALLE-D404", "Studio Son", 10, 0);

        Reservation r1 = new Reservation("RES-DEL-01", LocalDateTime.now(), LocalDateTime.now().plusHours(1), "Enregistrement A");
        Reservation r2 = new Reservation("RES-DEL-02", LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3), "Enregistrement B");
        r1.setSalle(salle);
        r2.setSalle(salle);

        user.addReservation(r1);
        user.addReservation(r2);

        em.persist(user);
        em.getTransaction().commit();

        Long r1Id = r1.getId();
        Long r2Id = r2.getId();
        Long userId = user.getId();

        // Étape 2 : Retirer r1 de la collection
        em.getTransaction().begin();
        Utilisateur managedUser = em.find(Utilisateur.class, userId);
        assertEquals(2, managedUser.getReservations().size());

        Reservation toRemove = managedUser.getReservations().stream()
                .filter(r -> r.getId().equals(r1Id))
                .findFirst()
                .orElse(null);

        assertNotNull(toRemove);
        managedUser.removeReservation(toRemove);
        em.getTransaction().commit();

        // Étape 3 : Vérifier que r1 a été supprimé de la base de données
        em.clear(); // Vider le cache de premier niveau
        Reservation deleted = em.find(Reservation.class, r1Id);
        Reservation kept = em.find(Reservation.class, r2Id);

        assertNull(deleted, "La réservation r1 retirée de la collection doit être supprimée de la base grâce à orphanRemoval");
        assertNotNull(kept, "La réservation r2 doit toujours exister");
    }

    @Test
    @Order(3)
    @DisplayName("Test 3 : Suppression utilisateur propage la suppression aux réservations mais préserve la salle")
    void testCascadeDeleteUserPreservesSalle() {
        em.getTransaction().begin();
        Utilisateur user = new Utilisateur("emma.watson@cinema.fr", "Watson", "Emma", "Art");
        Salle salle = new Salle("SALLE-E505", "Amphithéâtre Principal", 200, 1);

        Reservation res = new Reservation("RES-CAS-01", LocalDateTime.now(), LocalDateTime.now().plusHours(3), "Projection Film");
        res.setSalle(salle);
        user.addReservation(res);

        em.persist(user);
        em.getTransaction().commit();

        Long userId = user.getId();
        Long resId = res.getId();
        Long salleId = salle.getId();

        // Suppression de l'utilisateur
        em.getTransaction().begin();
        Utilisateur userToDelete = em.find(Utilisateur.class, userId);
        em.remove(userToDelete);
        em.getTransaction().commit();

        em.clear();
        assertNull(em.find(Utilisateur.class, userId), "L'utilisateur doit être supprimé");
        assertNull(em.find(Reservation.class, resId), "La réservation doit être supprimée en cascade");
        assertNotNull(em.find(Salle.class, salleId), "La salle NE DOIT PAS être supprimée lors de la suppression de l'utilisateur !");
    }

    @Test
    @Order(4)
    @DisplayName("Test 4 : Relation ManyToMany Salle <-> Équipement sans duplication d'équipements")
    void testManyToManySalleEquipement() {
        em.getTransaction().begin();

        Equipement ecran = new Equipement("EQ-DISP-99", "Écran Mobile 65\"", "Écran sur roulettes");

        Salle s1 = new Salle("SALLE-M1", "Salle Module 1", 12, 2);
        Salle s2 = new Salle("SALLE-M2", "Salle Module 2", 12, 2);

        s1.addEquipement(ecran);
        s2.addEquipement(ecran);

        em.persist(s1);
        em.persist(s2);
        em.getTransaction().commit();

        em.clear();
        Equipement reloadedEquipement = em.find(Equipement.class, ecran.getId());
        assertNotNull(reloadedEquipement);
        assertEquals(2, reloadedEquipement.getSalles().size(), "L'équipement doit être partagé entre les deux salles");
    }
}
