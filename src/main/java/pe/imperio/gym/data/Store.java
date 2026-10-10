package pe.imperio.gym.data;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Repository @Transactional
public class Store {
 @PersistenceContext private EntityManager em;
 public EntityManager em(){return em;}
 public <T> Optional<T> currentOne(Class<T> type,String jpql,Object... args){var q=em.createQuery(jpql,type).setLockMode(LockModeType.PESSIMISTIC_WRITE);for(int i=0;i<args.length;i++)q.setParameter(i+1,args[i]);return q.getResultList().stream().findFirst();}
 public <T> T save(T item){em.persist(item);return item;}
 public <T> T get(Class<T> type,Long id){T item=em.find(type,id);if(item==null)throw new IllegalArgumentException("El registro no existe.");return item;}
 public <T> T lock(Class<T> type,Long id){T item=em.find(type,id,LockModeType.PESSIMISTIC_WRITE);if(item==null)throw new IllegalArgumentException("El registro no existe.");return item;}
 public <T> List<T> list(Class<T> type,String jpql,Object... args){var q=em.createQuery(jpql,type);for(int i=0;i<args.length;i++)q.setParameter(i+1,args[i]);return q.getResultList();}
 public <T> Optional<T> one(Class<T> type,String jpql,Object... args){return list(type,jpql,args).stream().findFirst();}
 public int execute(String jpql,Object...args){var q=em.createQuery(jpql);for(int i=0;i<args.length;i++)q.setParameter(i+1,args[i]);return q.executeUpdate();}
 public long count(String jpql,Object...args){return list(Long.class,jpql,args).getFirst();}
}
