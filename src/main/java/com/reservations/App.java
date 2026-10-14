package com.reservations;

import com.reservations.model.Equipement;
import com.reservations.model.Reservation;
import com.reservations.model.Salle;
import com.reservations.model.Utilisateur;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class App {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("gestion-reservations-pu");
        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();

        Utilisateur user = new Utilisateur("Lucas Morel", "lucas.morel@etudiant.fr");
        Salle salle = new Salle("Salle 102");
        Equipement videoProj = new Equipement("Videoprojecteur");

        salle.addEquipement(videoProj);

        Reservation res1 = new Reservation("2024-11-20");
        res1.setSalle(salle);

        Reservation res2 = new Reservation("2024-11-25");
        res2.setSalle(salle);

        user.addReservation(res1);
        user.addReservation(res2);

        em.persist(user);

        em.getTransaction().commit();

        System.out.println("Utilisateur ajoute : id = " + user.getId());
        System.out.println("Salle ajoutee : id = " + salle.getId());

        em.getTransaction().begin();

        Salle salle2 = new Salle("Salle 204");
        salle2.addEquipement(videoProj);

        em.persist(salle2);

        em.getTransaction().commit();

        System.out.println("Salle 2 ajoutee avec equipement existant : " + videoProj.getNom());

        em.getTransaction().begin();

        Utilisateur u = em.find(Utilisateur.class, user.getId());
        if (!u.getReservations().isEmpty()) {
            Reservation r = u.getReservations().iterator().next();
            u.removeReservation(r);
        }

        em.getTransaction().commit();

        System.out.println("Suppression orpheline effectuee.");

        em.getTransaction().begin();

        Utilisateur toDelete = em.find(Utilisateur.class, user.getId());
        em.remove(toDelete);

        em.getTransaction().commit();

        System.out.println("Suppression de l'utilisateur effectuee.");

        em.close();
        emf.close();
    }
}
