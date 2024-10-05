package io.github.venkat1701.agnivbackend.dto.user;
import lombok.Data;
import java.util.List;

@Data
public class UserRegistrationDTO {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String role;
    private String phone;
    private List<ExperienceDTO> experienceList;
    private List<SkillDTO> skillList;

    @Data
    public static class ExperienceDTO {
        private String companyName;
        private String jobTitle;
        private String jobDescription;
        private String startDate;
        private String endDate;
    }

    @Data
    public static class SkillDTO {
        private String skill;
    }
}
