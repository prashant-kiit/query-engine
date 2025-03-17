select id, name, email, password, address 
from users 
where id >= 1 AND NOT name IN ("name1", "name2", "name3") OR name == "name2" AND address == "address1" OR address IN ("address2", "address3", "address4")
order_by id desc, name, address desc 
offset 5
limit 10