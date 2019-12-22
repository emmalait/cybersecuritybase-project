
package sec.project.repository;

import java.util.List;
import sec.project.domain.Signup;

public interface SignupRepositoryCustom {
    List<Signup> findByName(String name);
}
