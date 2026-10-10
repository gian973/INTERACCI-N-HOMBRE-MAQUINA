package pe.imperio.gym.domain;

import jakarta.persistence.*;

@Entity @Table(name="branches")
public class Branch {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="name") public String name;
 public String getName(){return name;}
 @Column(name="active") public boolean active;
 public boolean isActive(){return active;}
 @Column(name="capacity") public int capacity=30;
 public int getCapacity(){return capacity;}
}
