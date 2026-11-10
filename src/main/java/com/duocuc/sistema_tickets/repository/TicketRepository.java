package com.duocuc.sistema_tickets.repository;

import com.duocuc.sistema_tickets.model.Ticket;
import com.duocuc.sistema_tickets.repository.projection.EventSalesStats;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, String> {

	List<Ticket> findByEventId(String eventId);

	List<Ticket> findByUserId(String userId);

	List<Ticket> findByEventIdAndUserId(String eventId, String userId);

	@Query("""
			select t.eventId as eventId,
			       count(t) as ticketsGenerated,
			       sum(case when t.status = com.duocuc.sistema_tickets.model.TicketStatus.STORED
			           then 1 else 0 end) as ticketsUploaded,
			       sum(t.unitPrice) as totalSales
			from Ticket t
			where (:eventId is null or t.eventId = :eventId)
			group by t.eventId
			order by t.eventId
			""")
	List<EventSalesStats> getSalesStats(@Param("eventId") String eventId);
}
