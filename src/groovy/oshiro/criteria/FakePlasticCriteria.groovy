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
	
	def _clazz
	def _maxRes
	def _props = []
	def _groupProps = []
	def _orders = []
	def _distinctProp
	def _leCriticalList = [tp: 'and', ls: []]
	def _prefix = ''
	
	def _instanceValue
	
	def _criteriaValue
	def theImplementations = [
		'le':{ _instanceValue <= _criteriaValue },
		'lt':{ _instanceValue  < _criteriaValue },
		'gt':{ _instanceValue  > _criteriaValue },
		'ge':{ _instanceValue >= _criteriaValue },
		'eq':{ _instanceValue == _criteriaValue },
		'in':{ _instanceValue in _criteriaValue },
		'ne':{ _instanceValue != _criteriaValue },
		'ilike':{ ('' + _instanceValue).toLowerCase() ==~ _criteriaValue.replace('%','.*').toLowerCase() },
		'isNull':{ _instanceValue == null },
		'isNotNull':{ _instanceValue != null }
	]
	
	public FakePlasticCriteria(clazz){
		this._clazz = clazz
	}
	
	public FakePlasticCriteria(clazz, pref){
		_prefix = pref + '.'
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
		_maxRes = limit
	}
	
	def distinct(prop){
		_distinctProp = prop
	}
	
	def property(prop){
		_props.add(prop)
	}
	
	def sum(prop){
		_props.add("sum $prop")
	}
	
	def avg(prop){
		_props.add("avg $prop")
	}
	
	def groupProperty(prop){
		_groupProps.add(prop)
		_props.add(prop)
	}
	
	def projections(clos){
		clos.delegate = this
		clos()
	}
	
	def order(prop, order){
		_orders.add("${prop} ${order}")
	}
	
	def methodMissing(String name, args){
		if(theImplementations.containsKey(name)){
			_leCriticalList.ls.add([cp: name, prop: _prefix + args[0], val: ((args.length > 1) ? args[1] : 'null')])
		}else{
			if(!(args[0] instanceof Closure)) throw new RuntimeException("metodo ${name} nao foi implementado")
			def fc = new FakePlasticCriteria(this._clazz, name)
			args[0].resolveStrategy = Closure.DELEGATE_FIRST
			args[0].delegate = fc
			args[0]()
			fc._leCriticalList.ls.each{ v ->
				this._leCriticalList.ls.add(v)
			}
		}
	}
	
	def and(clos){
		def thePersistenceOfMemory = _leCriticalList
		_leCriticalList = [tp: 'and', ls: []]
		thePersistenceOfMemory.ls.add(_leCriticalList)
		clos.delegate = this
		clos()
		_leCriticalList = thePersistenceOfMemory
	}
	
	def or(clos){
		def thePersistenceOfMemory = _leCriticalList
		_leCriticalList = [tp: 'or', ls: []]
		thePersistenceOfMemory.ls.add(_leCriticalList)
		clos.delegate = this
		clos()
		_leCriticalList = thePersistenceOfMemory
	}
	
	def list(clos){
		clos.delegate = this
		clos()
		def ls = filteredList()
		if(_props){
			def rs = []
			def extractProps = { vls ->
				def rsItem = []
				_props.each{ prop ->
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
				rs.add(_props.size() == 1 ? rsItem[0] : rsItem)
			}
			if(_groupProps){
				ls.groupBy{ item ->
					_groupProps.collect{ groupProp ->
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
		} else if(_distinctProp){
			def rs = []
			ls.each{
				if(!rs.contains(it."$_distinctProp")) rs.add(it."$_distinctProp")
			}
			ls = rs
		}
		return ls
	}
	
	def whereIsMyMind(criList, obj){
		if(criList.tp == 'and'){
			return !criList.ls.any{ cri ->
				if(cri.cp){
					_criteriaValue = cri.val
					_instanceValue = obj
					cri.prop.split('\\.').each{ _instanceValue = it == 'class' ? _instanceValue.class.name : _instanceValue."$it"	}
					return !theImplementations[cri.cp]()
				}else{
					return !whereIsMyMind(cri, obj)
				}
			}
		} else if(criList.tp == 'or'){
			return criList.ls.any{ cri ->
				if(cri.cp){
					_criteriaValue = cri.val
					_instanceValue = obj
					cri.prop.split('\\.').each{ _instanceValue = it == 'class' ? _instanceValue.class.name : _instanceValue."$it"	}
					return theImplementations[cri.cp]()
				}else{
					return whereIsMyMind(cri, obj)
				}
			}
		} else{
			throw new RuntimeException("Foo bar")
		}
	}

	def filteredList(){
		def r = []
		_clazz.list().each{ obj ->
			if(whereIsMyMind(_leCriticalList, obj)){
				r.add(obj)
			}
		}
		_orders.each{
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

