package oshiro.criteria;

/**
 * Agora que o Grails 2 chegou esse código não tem mais utilidade.
 * 
 *  "Prefiro as máquinas que servem para não funcionar: 
 *   quando cheias de areia, de formiga e musgo - elas podem um dia milagrar de flores. 
 *   Também as latrinas desprezadas que servem para ter grilos dentro - elas podem um dia milagrar violetas. 
 *   Senhor, eu tenho orgulho do imprestável."
 * 
 *                 Manoel de Barros
 *                 
 */
class FakePlasticCriteria {
	
	def clazz
	def maxRes
	def comparatorsMethods = ['le', 'ne', 'lt', 'gt', 'ge', 'eq', 'in', 'isNull', 'isNotNull', 'ilike']
	def comparatorsMaps = [:]
	def props = []
	def groupProps = []
	def orders = []
	def distinctProp 
	
	public FakePlasticCriteria(clazz){
		this.clazz = clazz
	}
	
	static void mockCriteria(clazz){
		clazz.metaClass.'static'.withCriteria = { cls ->
			new FakePlasticCriteria(clazz).list(cls)
		}
		clazz.metaClass.'static'.createCriteria = {
			new FakePlasticCriteria(clazz)
		}
	}
	
	def listDistinct(cls){
		
	}
	
	def maxResults(limit){
		maxRes = limit
	}
	
	def distinct(prop){
		distinctProp = prop
	}
	
	def property(prop){
		props.add(prop)
	}
	
	def sum(prop){
		props.add("sum $prop")
	}
	
	def avg(prop){
		props.add("avg $prop")
	}
	
	def groupProperty(prop){
		groupProps.add(prop)
		props.add(prop)
	}
	
	def projections(clos){
		clos.delegate = this
		clos()
	}
	
	def order(prop, order){
		orders.add("${prop} ${order}")
	}
	
	def methodMissing(String name, args){
		if(comparatorsMethods.contains(name)){
			comparatorsMaps.get(name, [:]).put(args[0], (args.length > 1) ? args[1] : 'null')
		}else{
			def fc = new FakePlasticCriteria(this.clazz)
			if(!(args[0] instanceof Closure)) throw new RuntimeException("metodo ${name} nao foi implementado")
			args[0].resolveStrategy = Closure.DELEGATE_FIRST
			args[0].delegate = fc
			args[0]()
			fc.comparatorsMaps.each{ k, v ->
				v.each{ sk, sv ->
					this.comparatorsMaps.get(k, [:]).put("${name}."+sk, sv)
				}
			}
		}
	}
	
	def or(clos){
	
	}
	
	def list(clos){
		clos.delegate = this
		clos()
		def ls = filteredList()
		if(props){
			def rs = []
			def extractProps = { vls ->
				def rsItem = []
				props.each{ prop ->
					if(prop.startsWith('sum ')){
						rsItem.add(vls.sum(0.0){it."${prop.substring(4)}"})
					}else if(prop.startsWith('avg ')){
						rsItem.add(vls.sum(0.0){it."${prop.substring(4)}"} / vls.size())
					}else{
						def gp = vls.first()
						prop.split('\\.').each{ gp = gp."$it" }
						rsItem.add(gp)
					}
				}
				rs.add(props.size() == 1 ? rsItem[0] : rsItem)
			}
			if(groupProps){
				ls.groupBy{ item ->
					groupProps.collect{ groupProp ->
						def gp = item
						groupProp.split('\\.').each{ gp = gp."$it" }
						return gp
					}
				}.each{ k, vls ->
					extractProps(vls)
				}
			}else{
				extractProps(ls)
			}
			ls = rs
		} else if(distinctProp){
			def rs = []
			ls.each{
				if(!rs.contains(it."$distinctProp")) rs.add(it."$distinctProp")
			}
			ls = rs
		}
		return ls
	}
	
	def filteredList(){
		def r = []
		def currentObj
		def oVal
		def cVal
		def doCrit = { op, cls -> 
			!comparatorsMaps[op]?.any{ prop, val ->
				cVal = val
				oVal = currentObj
				prop.split('\\.').each{ oVal = it == 'class' ? oVal.class.name : oVal."$it"	}
				!cls() 
			}
		}
		clazz.list().each{ obj ->
			currentObj = obj
			if(
				doCrit('le'){ oVal <= cVal } &&
				doCrit('lt'){ oVal  < cVal } &&
				doCrit('gt'){ oVal  > cVal } &&
				doCrit('ge'){ oVal >= cVal } &&
				doCrit('eq'){ oVal == cVal } && 
				doCrit('in'){ oVal in cVal } &&
				doCrit('ne'){ oVal != cVal } &&
				doCrit('ilike'){ ('' + oVal).toLowerCase() ==~ cVal.replace('%','.*').toLowerCase() } &&
				doCrit('isNull'){ oVal == null } &&
				doCrit('isNotNull'){ oVal != null }
			){
				r.add(obj)
			}
		}
		orders.each{
			def arr = it.split(' ')
			def prop = arr[0]
			def order = arr[1]
			r.sort{a, b->
				try{
					if(order == 'asc'){
						return a."$prop" - b."$prop"
					} else {
						return b."$prop" - a."$prop"
					}
				}catch(e){
					return 0
				}
			}
		}
		return r
	}
	
	def get(clos){
		def ls = list(clos)
		return ls ? ls.first() : null
	}
}
