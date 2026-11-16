package com.hexisnutrition.backend.notifiche;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {

    List<PushSubscription> findAllByProfessionistaId(UUID professionistaId);

    Optional<PushSubscription> findByEndpoint(String endpoint);

    void deleteByEndpointAndProfessionistaId(String endpoint, UUID professionistaId);

    // Chiamato fuori da una transazione (invio push dello scheduler): la apre il repository.
    @Transactional
    void deleteByEndpoint(String endpoint);
}
