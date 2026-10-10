INSERT INTO roles(id,name) VALUES(1,'CLIENTE'),(2,'ADMIN');
INSERT INTO plans(name,months,price,active) VALUES('Plan mensual',1,89.99,true),('Plan 2 meses',2,114.99,true),('Plan 3 meses',3,179.99,true);
INSERT INTO benefits(label) VALUES('Sala de musculación'),('Clases grupales'),('Acceso a todas las sedes'),('Portal de autoservicio');
INSERT INTO plan_benefits(plan_id,benefit_id) SELECT p.id,b.id FROM plans p CROSS JOIN benefits b;
