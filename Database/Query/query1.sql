select id, count(name), sum(email), password, address 
from users 
where id >= 1 AND NOT name IN ("name1", "name2", "name3") OR name == "name2" AND address == "address1" OR address IN ("address2", "address3", "address4")
group_by password, adddress
having X > 1 AND Y > 1
order_by id desc, name, address desc
offset 5
limit 10