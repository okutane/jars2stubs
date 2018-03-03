package $package;

#foreach($import in $imports)
import $import;
#end

public $classKind $className {
    #foreach($field in $fields)
        $field.flags $field.type $field.name #if($field.initializer) = $field.initializer#end;
    #end

    #foreach($method in $methods)
        $method.flags $method.returnType $method.name (#foreach($parameterType in $method.parameterTypes)$parameterType arg$foreach.index #if($foreach.hasNext ), #end#end)#if($method.hasBody) {

        }#else;#end

    #end
}
