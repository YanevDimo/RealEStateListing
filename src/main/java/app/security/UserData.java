package app.security;



import app.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UserData implements UserDetails {

    private UUID userId;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private UserRole role;
    private boolean isAccontActive;



    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        String roleName = "ROLE_" + role.name();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleName);

        return List.of(authority);
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean isEnabled() {
        return this.isAccontActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.isAccontActive;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.isAccontActive;
    }
    @Override
    public boolean isAccountNonLocked() {
        return this.isAccontActive;
    }

}
