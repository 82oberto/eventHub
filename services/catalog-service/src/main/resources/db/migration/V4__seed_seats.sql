-- Posti fisici: 5 file x 10 posti per ogni venue
INSERT INTO seats (venue_id, sector, row_label, seat_number, category)
SELECT v.id,
       'A',
       r.row_label,
       s.seat_number,
       CASE WHEN r.row_label IN ('A','B') THEN 'PREMIUM' ELSE 'STANDARD' END
FROM venues v
         CROSS JOIN (VALUES ('A'),('B'),('C'),('D'),('E')) AS r(row_label)
         CROSS JOIN generate_series(1,10) AS s(seat_number);

-- Disponibilità e prezzo per ogni evento
INSERT INTO event_seats (event_id, seat_id, price, status)
SELECT e.id,
       s.id,
       CASE WHEN s.category = 'PREMIUM' THEN 89.00 ELSE 49.00 END,
       'AVAILABLE'
FROM events e
         JOIN seats s ON s.venue_id = e.venue_id;