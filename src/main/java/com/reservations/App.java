package com.reservations;

import com.reservations.model.Equipement;
import com.reservations.model.Reservation;
import com.reservations.model.Salle;
import com.reservations.model.Utilisateur;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;

public class App {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion-reservations-pu");
        EntityManager em = emf.createEntityManager();

        Long userId;
        Long res1Id;
        Long res2Id;
        Long salleId;
        Long equipementId;

        try {
            em.getTransaction().begin();

            Utilisateur user1 = new Utilisateur("Marc Lefebvre", "marc.lefebvre@univ.fr", "0612345678");

            Salle salle1 = new Salle("Salle B101", 30);

            Equipement proj = new Equipement("Projecteur HD", "Projecteur fixe mural");
            Equipement ecran = new Equipement("Ecran Interactif", "Ecran tactile");

            salle1.addEquipement(proj);
            salle1.addEquipement(ecran);

            Reservation r1 = new Reservation(
                    LocalDateTime.now().plusDays(1).withHour(10).withMinute(0),
                    LocalDateTime.now().plusDays(1).withHour(12).withMinute(0),
                    "Reunion pedagogique"
            );
            r1.setSalle(salle1);

            Reservation r2 = new Reservation(
                    LocalDateTime.now().plusDays(2).withHour(14).withMinute(0),
                    LocalDateTime.now().plusDays(2).withHour(16).withMinute(0),
                    "Session de formation"
            );
            r2.setSalle(salle1);

            user1.addReservation(r1);
            user1.addReservation(r2);

            em.persist(user1);

            em.getTransaction().commit();

            userId = user1.getId();
            res1Id = r1.getId();
            res2Id = r2.getId();
            salleId = salle1.getId();
            equipementId = proj.getId();

            System.out.println("Utilisateur insere avec ID : " + userId);
            System.out.println("Reservation 1 ID : " + res1Id + ", Reservation 2 ID : " + res2Id);
            System.out.println("Salle inseree avec ID : " + salleId);
            System.out.println("Equipement insere avec ID : " + equipementId);

            em.getTransaction().begin();

            Salle salle2 = new Salle("Salle C202", 15);
            Equipement existingProj = em.find(Equipement.class, equipementId);
            salle2.addEquipement(existingProj);

            em.persist(salle2);
            em.getTransaction().commit();

            System.out.println("Salle 2 associee a l'equipement existant : " + existingProj.getNom());

            em.getTransaction().begin();

            Utilisateur u = em.find(Utilisateur.class, userId);
            Reservation toDelete = null;
            for (Reservation r : u.getReservations()) {
                if (r.getId().equals(res1Id)) {
                    toDelete = r;
                    break;
                }
            }

            if (toDelete != null) {
                u.removeReservation(toDelete);
            }

            em.getTransaction().commit();
            em.clear();

            Reservation verifRes1 = em.find(Reservation.class, res1Id);
            System.out.println("Reservation 1 apres orphanRemoval (doit etre null) : " + verifRes1);

            em.getTransaction().begin();

            Utilisateur user2 = new Utilisateur("Sophie Bernard", "sophie.b@univ.fr", "0698765432");
            Salle salleForUser2 = em.find(Salle.class, salleId);

            Reservation r3 = new Reservation(
                    LocalDateTime.now().plusDays(3).withHour(9).withMinute(0),
                    LocalDateTime.now().plusDays(3).withHour(11).withMinute(0),
                    "Soutenance de stage"
            );
            r3.setSalle(salleForUser2);
            user2.addReservation(r3);

            em.persist(user2);
            em.getTransaction().commit();

            Long user2Id = user2.getId();
            Long r3Id = r3.getId();

            em.getTransaction().begin();
            Utilisateur toRemove = em.find(Utilisateur.class, user2Id);
            em.remove(toRemove);
            em.getTransaction().commit();
            em.clear();

            Reservation verifR3 = em.find(Reservation.class, r3Id);
            Salle verifSalle = em.find(Salle.class, salleId);

            System.out.println("Reservation 3 supprimee avec l'utilisateur (doit etre null) : " + verifR3);
            System.out.println("Salle toujours existante : " + (verifSalle != null ? verifSalle.getNom() : "null"));

        } finally {
            em.close();
            emf.close();
        }
    }
}
