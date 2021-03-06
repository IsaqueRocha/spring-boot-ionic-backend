package com.example.cursomvc.services;

import com.example.cursomvc.domain.Cidade;
import com.example.cursomvc.domain.Cliente;
import com.example.cursomvc.domain.Endereco;
import com.example.cursomvc.domain.enums.Perfil;
import com.example.cursomvc.domain.enums.TipoCliente;
import com.example.cursomvc.dto.ClienteDTO;
import com.example.cursomvc.dto.ClienteNewDTO;
import com.example.cursomvc.repositories.ClienteRepository;
import com.example.cursomvc.repositories.EnderecoRepository;
import com.example.cursomvc.security.UserSS;
import com.example.cursomvc.services.exceptions.AuthorizationException;
import com.example.cursomvc.services.exceptions.DataIntegrityException;
import com.example.cursomvc.services.exceptions.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClienteService {
    @Autowired
    BCryptPasswordEncoder bCrypt;

    @Autowired
    private ClienteRepository repo;

    @Autowired
    EnderecoRepository enderecoRepository;

    public List<Cliente> findAll() {
        return repo.findAll();
    }


    public Cliente find(Integer id) {

        UserSS user = UserService.authenticated();

        if (user == null || !user.hasHole(Perfil.ADMIN) && !id.equals(user.getId())) {
            throw new AuthorizationException("Acesso Negado!");
        }

        Optional<Cliente> obj = repo.findById(id);
        return obj.orElseThrow(() -> new ObjectNotFoundException(
                "Objeto não encontrado! ID: " + id + " , Tipo: " + Cliente.class.getName())
        );
    }

    @Transactional
    public Cliente insert(Cliente obj) {
        obj.setId(null);
        obj = repo.save(obj);
        enderecoRepository.saveAll(obj.getEnderecos());
        return obj;
    }


    public Cliente update(Cliente obj) {
        Cliente newObj = find(obj.getId());
        updateData(newObj, obj);
        return repo.save(newObj);
    }

    public void delete(Integer id) {
        find(id);
        try {
            repo.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DataIntegrityException("Não é possível excluir porque há pedidos relacionados");
        }
    }

    public Page<Cliente> findPage(Integer page, Integer linesPerPage, String orderBy, String direction) {
        PageRequest pageRequest = PageRequest.of(page, linesPerPage, Sort.Direction.valueOf(direction), orderBy);

        return repo.findAll(pageRequest);
    }

    public Cliente fromDTO(ClienteDTO objDto) {
        return new Cliente(objDto.getId(), objDto.getNome(), objDto.getEmail(), null, null, null);
    }

    public Cliente fromDTO(ClienteNewDTO objDto) {
        Cliente cli = new Cliente(
                null,
                objDto.getNome(),
                objDto.getEmail(),
                objDto.getCpfOuCnpj(),
                TipoCliente.toEnum(objDto.getTipo()),
                bCrypt.encode(objDto.getSenha())
        );

        Cidade cid = new Cidade(objDto.getCidadeId(), null, null);

        Endereco end = new Endereco(
                null,
                objDto.getLogradouro(),
                objDto.getNumero(),
                objDto.getComplemento(),
                objDto.getBairro(),
                objDto.getCep(),
                cli,
                cid
        );

        cli.getEnderecos().add(end);

        cli.getTelefones().add(objDto.getTelefone1());

        if (objDto.getTelefone2() != null) {
            cli.getTelefones().add(objDto.getTelefone2());
        }

        if (objDto.getTelefone3() != null) {
            cli.getTelefones().add(objDto.getTelefone3());
        }

        return cli;
    }

    private void updateData(Cliente newObj, Cliente obj) {
        newObj.setNome(obj.getNome());
        newObj.setEmail(obj.getEmail());

    }
}
