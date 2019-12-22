package sec.project.repository;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import sec.project.domain.Signup;

public class SignupRepositoryImpl implements SignupRepositoryCustom {
    @PersistenceContext
    private EntityManager em;
    
    @Override
    public List<Signup> findByName(String name) {
        List<Signup> signups = em.createNativeQuery("SELECT * FROM Signup WHERE name = '" + name + "'", Signup.class).getResultList();
        return signups;
        }
    
}
