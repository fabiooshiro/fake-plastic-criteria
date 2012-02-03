package oshiro.criteria;

import grails.test.GrailsUnitTestCase;
import static oshiro.criteria.FakePlasticCriteria.mockCriteria;


class FakePlasticCriteriaTests extends GrailsUnitTestCase {
	
    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }
	
	void testAvg(){
		def plastic = new Plastic(name: 'The Persistence of Memory')
		mockDomain(Tree, [
			new Tree(material: plastic, value: 1, color: 'Green'),
			new Tree(material: plastic, value: 2, color: 'Red'),
			new Tree(material: plastic, value: 3, color: 'Blue'),
			new Tree(material: plastic, value: 4, color: 'Orange'),
			new Tree(material: plastic, value: 5, color: 'Black'),
		])
		
		mockCriteria(Tree)
		
		def ls = Tree.withCriteria{
			projections {
				avg('value')
				groupProperty('material.name')
			}
		}
		assert [[3.0, 'The Persistence of Memory']] == ls
	}
   
	void testSum(){
		def plastic = new Plastic(name: 'oil 42')
		mockDomain(Tree, [
			new Tree(material: plastic, value: 1, color: 'Green'),
			new Tree(material: plastic, value: 2, color: 'Red'),
			new Tree(material: plastic, value: 3, color: 'Blue'),
			new Tree(material: plastic, value: 4, color: 'Orange'),
			new Tree(material: plastic, value: 5, color: 'Black'),
		])
		
		mockCriteria(Tree)
		
		def ls = Tree.withCriteria{
			projections {
				sum('value')
				groupProperty('material.name')
			}
		}
		assert [[15.0, 'oil 42']] == ls
	}
	
	void testEq(){
		def plastic1 = new Plastic(name: 'oil on canvas')
		def plastic2 = new Plastic(name: 'oil 42')
		mockDomain(Tree, [
			new Tree(material: plastic1, value: 1, color: 'Green'),
			new Tree(material: plastic1, value: 2, color: 'Red'),
			new Tree(material: plastic2, value: 3, color: 'Blue'),
			new Tree(material: plastic2, value: 4, color: 'Orange'),
			new Tree(material: plastic2, value: 5, color: 'Black'),
		])
		
		mockCriteria(Tree)
		
		def ls = Tree.withCriteria{
			material{
				eq('name', 'oil on canvas')
			}
		}
		
		assert ['Green', 'Red'] == ls.collect{ it.color }
	}
	
	void 'test Salvador Dali or Monet'(){
		def plastic1 = new Plastic(name: 'Soleil levant')
		def plastic2 = new Plastic(name: 'The Madonna of Port Lligat')
		def plastic3 = new Plastic(name: "Les Demoiselles d'Avignon")
		mockDomain(Plastic, [plastic1, plastic2, plastic3])
		mockCriteria(Plastic)
		def ls = Plastic.withCriteria{
			or{
				eq('name', 'Soleil levant')
				eq('name', 'The Madonna of Port Lligat')
			}
		}
		
		assert [plastic1, plastic2] == ls
	}
	
	void 'test Salvador Dali or Monet and price'(){
		def plastic1 = new Plastic(name: 'Soleil levant', price: 10)
		def plastic2 = new Plastic(name: 'The Madonna of Port Lligat', price: 11)
		def plastic3 = new Plastic(name: "Les Demoiselles d'Avignon", price: 12)
		mockDomain(Plastic, [plastic1, plastic2, plastic3])
		mockCriteria(Plastic)
		def ls = Plastic.withCriteria{
			or{
				eq('name', 'Soleil levant')
				eq('name', 'The Madonna of Port Lligat')
			}
			lt('price', 11)
		}
		
		assert [plastic1] == ls
	}
	
	void 'test Salvador Dali or Monet and price 02'(){
		def plastic1 = new Plastic(name: 'Soleil levant', price: 10)
		def plastic2 = new Plastic(name: 'The Madonna of Port Lligat', price: 11)
		def plastic3 = new Plastic(name: "Les Demoiselles d'Avignon", price: 11)
		mockDomain(Plastic, [plastic1, plastic2, plastic3])
		mockCriteria(Plastic)
		def ls = Plastic.withCriteria{
			or{
				eq('name', 'Soleil levant')
				and{
					eq('name', 'The Madonna of Port Lligat')
					eq('price', 11)
				}
			}
		}
		
		assert [plastic1, plastic2] == ls
	}
}